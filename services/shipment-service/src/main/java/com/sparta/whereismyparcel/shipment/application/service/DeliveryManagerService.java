package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import com.sparta.whereismyparcel.shipment.domain.exception.NoAvailableDeliveryManagerException;
import com.sparta.whereismyparcel.shipment.domain.repository.DeliveryManagerRepository;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerService {

    private final DeliveryManagerPolicy deliveryManagerPolicy;
    private final DeliveryManagerRepository deliveryManagerRepository;

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

    public List<UUID> assignHubDeliveryManagers(int count) {
        return assign(null, DeliveryType.HUB_DELIVERY, count);
    }

    public List<UUID> assignCompanyDeliveryManagers(UUID hubId, int count) {
        return assign(hubId, DeliveryType.COMPANY_DELIVERY, count);
    }

    private List<UUID> assign(UUID hubId, DeliveryType type, int count) {
        List<DeliveryManager> deliveryManagers = switch (type) {
            // 배송 타입에 따라 배정 대상 배송 담당자 조회
            case HUB_DELIVERY ->
                    deliveryManagerRepository.findNextHubDeliveryManagers(
                            type,
                            Pageable.ofSize(count)
                    );

            case COMPANY_DELIVERY ->
                    deliveryManagerRepository.findNextCompanyDeliveryManagers(
                            hubId,
                            type,
                            Pageable.ofSize(count)
                    );
        };
        // 배정 가능한 배송 담당자가 없는 경우 예외 발생
        if (deliveryManagers.isEmpty()) {
            throw new NoAvailableDeliveryManagerException();
        }
        // 배송 담당자 ID 목록 반환
        return deliveryManagers.stream()
                .map(DeliveryManager::getId)
                .toList();
    }
}
