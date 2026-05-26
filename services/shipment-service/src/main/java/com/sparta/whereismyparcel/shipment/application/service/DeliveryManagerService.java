package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.common.exception.BusinessException;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import com.sparta.whereismyparcel.shipment.domain.exception.*;
import com.sparta.whereismyparcel.shipment.domain.repository.DeliveryManagerRepository;
import com.sparta.whereismyparcel.shipment.infrastructure.client.HubClient;
import com.sparta.whereismyparcel.shipment.infrastructure.client.UserClient;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerSearchRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerUpdateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerCreateResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerViewResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerService {

    private final DeliveryManagerPolicy deliveryManagerPolicy;
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final UserClient userClient;
    private final HubClient hubClient;


    @Transactional
    public DeliveryManagerCreateResponse create(DeliveryManagerCreateRequest request) {
        //1. 생성 정책 확인
        deliveryManagerPolicy.checkCreate(request.slackId(), request.type(), request.hubId());
        //2. 배송담당자 순번 채번
        int newOrder = getNextDeliveryOrder(request.hubId(), request.type());
        //3. 저장
        DeliveryManager saved = deliveryManagerRepository.save(
                DeliveryManager.create(request.hubId(), request.slackId(), request.type(), newOrder)
        );

        return DeliveryManagerCreateResponse.from(saved);
    }

    private int getNextDeliveryOrder(UUID hubId, DeliveryType type) {
        return switch (type) {
            case HUB_DELIVERY -> deliveryManagerRepository.findNextOrderByHub(type);
            case COMPANY_DELIVERY -> {
                yield deliveryManagerRepository.findNextOrderByCompany(hubId, type);
            }
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    //region [배송 담당자 배정]
    public List<UUID> assignHubDeliveryManagers(int count) {
        return assign(null, DeliveryType.HUB_DELIVERY, count);
    }

    public List<UUID> assignCompanyDeliveryManagers(UUID hubId, int count) {
        return assign(hubId, DeliveryType.COMPANY_DELIVERY, count);
    }

    private List<UUID> assign(UUID hubId, DeliveryType type, int count) {
        List<DeliveryManager> deliveryManagers = switch (type) {
            // 배송 타입에 따라 배정 대상 배송 담당자 조회
            case HUB_DELIVERY -> deliveryManagerRepository.findNextHubDeliveryManagers(
                    type,
                    Pageable.ofSize(count)
            );

            case COMPANY_DELIVERY -> deliveryManagerRepository.findNextCompanyDeliveryManagers(
                    hubId,
                    type,
                    Pageable.ofSize(count)
            );
        };

        // 배정 가능한 배송 담당자 수가 필요한 수보다 적은 경우 예외 발생
        if (deliveryManagers.size() < count) {
            throw new NoAvailableDeliveryManagerException();
        }

        // 배송 담당자 ID 목록 반환
        return deliveryManagers.stream()
                .map(DeliveryManager::getId)
                .toList();
    }
    //endregion

    @Transactional
    public void update(String userId, UUID deliveryManagerId, DeliveryManagerUpdateRequest request) {
        DeliveryManager deliveryManager = deliveryManagerRepository.findById(deliveryManagerId)
                .orElseThrow(DeliveryManagerNotFoundException::new);

        checkWritePermission(UUID.fromString(userId), deliveryManager);

        validateHubIfCompanyDelivery(request.type(), request.hubId());

        deliveryManager.update(request.hubId(), request.slackId(), request.type());
    }

    private void validateHubIfCompanyDelivery(DeliveryType type, UUID hubId) {
        //허브 담당 배송자의 경우, 유효한 허브인지 확인
        if (type != DeliveryType.COMPANY_DELIVERY) {
            return;
        }

        boolean exists = hubClient.exists(hubId).data();

        if (!exists) {
            throw new HubNotFoundException();
        }
    }


    @Transactional
    public void delete(String userId, UUID deliveryManagerId) {
        DeliveryManager deliveryManager = deliveryManagerRepository.findById(deliveryManagerId)
                .orElseThrow(DeliveryManagerNotFoundException::new);

        checkWritePermission(UUID.fromString(userId), deliveryManager);

        deliveryManager.delete(userId);
    }

    public DeliveryManagerViewResponse getDeliveryManager(String userId, UUID deliveryManagerId) {
        DeliveryManager deliveryManager = deliveryManagerRepository.findById(deliveryManagerId)
                .orElseThrow(DeliveryManagerNotFoundException::new);

        checkReadPermission(UUID.fromString(userId), deliveryManager);
        return DeliveryManagerViewResponse.from(deliveryManager);
    }

    public Page<DeliveryManagerViewResponse> search(
            String userId,
            DeliveryManagerSearchRequest request,
            Pageable pageable
    ) {
        return deliveryManagerRepository.search(request, pageable)
                .map(DeliveryManagerViewResponse::from);
    }

    private void checkWritePermission(UUID userId, DeliveryManager manager) {
        // 담당 허브 관리자인지 확인
        validateHubPermission(userId, manager, DeliveryManagerWriteDeniedException::new);
    }

    private void checkReadPermission(UUID userId, DeliveryManager manager) {
        // 담당 허브 관리자인지 확인
        validateHubPermission(userId, manager, DeliveryManagerReadDeniedException::new);

        // 본인 slack ID 존재 여부 검진 (userClient 내부에서 예외 발생)
        userClient.exists(manager.getSlackId());
    }

    /**
     * 딤딩 허브 관리자인지 확인
     */
    private void validateHubPermission(UUID userId, DeliveryManager manager, Supplier<BusinessException> exceptionSupplier) {
        UserResponse user = userClient.getUser(userId).data();
        if (!user.hubId().equals(manager.getHubId())) {
            throw exceptionSupplier.get();
        }
    }
}
