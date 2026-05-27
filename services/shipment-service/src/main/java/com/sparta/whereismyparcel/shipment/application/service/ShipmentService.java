package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.domain.entity.ShipmentHistory;
import com.sparta.whereismyparcel.shipment.domain.entity.ShipmentItem;
import com.sparta.whereismyparcel.shipment.domain.entity.ShipmentStatus;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentAlreadyStartedException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentNotFoundException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentRouteNotFoundException;
import com.sparta.whereismyparcel.shipment.domain.exception.ShipmentUpdateDeniedException;
import com.sparta.whereismyparcel.shipment.domain.repository.ShipmentRepository;
import com.sparta.whereismyparcel.shipment.domain.util.ShipmentNumberGenerator;
import com.sparta.whereismyparcel.shipment.infrastructure.client.*;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.GetDestinationHubIdRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.ShipmentCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.ShipmentSearchRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.GetProductHubIdResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.ShipmentCreateResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.ShipmentViewResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.ShortestPathResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final DeliveryManagerService deliveryManagerService;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final CompanyClient companyClient;
    private final HubClient hubClient;

    @Transactional
    public void cancel(String userId, UUID orderId) {
        UUID managerId = UUID.fromString(userId);
        //1. 주문에 속한 모든 배송들 조회
        List<Shipment> shipments = shipmentRepository.findAllByOrderId(orderId);

        //2. 권한 체크
        validateUpdatePermission(shipments, managerId);

        //3. 주문에 속한 모든 배송들 출발 전인지 확인
        boolean allCancelable = shipments.stream()
                .allMatch(Shipment::canCancel);

        if (!allCancelable) {
            throw new ShipmentAlreadyStartedException();
        }
        //4. 취소 처리
        shipments.forEach(Shipment::cancel);
    }

    @Transactional
    public void delivered(String userId, UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(ShipmentNotFoundException::new);

        //1. 수정 권한 체크
        validateUpdatePermission(List.of(shipment), UUID.fromString(userId));

        //2. 완료 처리
        shipment.delivered();

        //3. 주문에 속한 모든 배송들 완료됐는지 확인
        List<Shipment> shipments = shipmentRepository.findAllByOrderId(shipment.getOrderId());

        boolean allDelivered = shipments.stream()
                .allMatch(Shipment::isDelivered);

        //4. 모두 배송완료됐으면 주문 완료 처리 api 요청
        if (allDelivered) {
            orderClient.complete(userId, shipment.getOrderId());
        }
    }

    //region [배송 생성]
    @Transactional
    public List<ShipmentCreateResponse> create(String userId, ShipmentCreateRequest request) {

        // 상품 옵션 ID 추출
        Set<UUID> productVariantIds = extractProductVariantIds(request);

        // 상품별 출발 허브 조회
        List<GetProductHubIdResponse> hubMappings =
                productClient.getHubMappingsByProductIds(userId, productVariantIds)
                        .data();

        // 배송지 기준 최종 도착 허브 조회
        UUID destinationHubId = getDestinationHubId(request);

        // 출발 허브 기준 상품 그룹핑
        Map<UUID, List<UUID>> productsByHub = groupProductsByHub(hubMappings);

        // 출발 허브별 배송 생성
        List<Shipment> shipments = productsByHub.entrySet().stream()
                .map(entry -> createShipment(
                        request,
                        entry.getKey(),
                        destinationHubId
                ))
                .toList();

        // 배송 저장 및 응답 반환
        return shipmentRepository.saveAll(shipments).stream()
                .map(ShipmentCreateResponse::from)
                .toList();
    }

    /**
     * 요청 상품 옵션 ID 추출
     */
    private Set<UUID> extractProductVariantIds(ShipmentCreateRequest request) {
        return request.items().stream()
                .map(ShipmentCreateRequest.Item::productVariantId)
                .collect(toSet());
    }

    /**
     * 배송지 기반 최종 도착 허브 조회
     */
    private UUID getDestinationHubId(ShipmentCreateRequest request) {
        return companyClient.getDestinationHubId(
                        new GetDestinationHubIdRequest(
                                request.zipCode(),
                                request.address(),
                                request.addressDetail()
                        )
                )
                .data();
    }

    /**
     * 출발 허브 기준 상품 그룹핑
     */
    private Map<UUID, List<UUID>> groupProductsByHub(
            List<GetProductHubIdResponse> hubMappings
    ) {
        return hubMappings.stream()
                .collect(groupingBy(
                        GetProductHubIdResponse::hubId,
                        mapping(GetProductHubIdResponse::productVariantId, toList())
                ));
    }

    /**
     * 배송 생성
     */
    private Shipment createShipment(
            ShipmentCreateRequest request,
            UUID originHubId,
            UUID destinationHubId
    ) {

        // 허브 간 최단 경로 조회
        var shortestPath = Optional.ofNullable(
                        hubClient.getShortestPath(originHubId, destinationHubId)
                )
                .map(ApiResponse::data)
                .orElseThrow(ShipmentRouteNotFoundException::new);

        List<ShortestPathResponse.RouteSegmentResponse> routes = shortestPath.routes();

        // 업체 배송 담당자 배정
        UUID companyManagerId =
                deliveryManagerService.assignCompanyDeliveryManagers(destinationHubId, 1)
                        .get(0);

        // 배송 생성
        Shipment shipment = Shipment.create(
                request.orderId(),
                originHubId,
                destinationHubId,
                companyManagerId,
                ShipmentNumberGenerator.generate(),
                ShipmentStatus.HUB_WAITING,
                buildAddress(request),
                request.recipientName(),
                request.recipientPhone()
        );

        //배송 상품 생성
        List<ShipmentItem> items = createShipmentItems(shipment, request.items());

        // 배송 이력 생성
        List<ShipmentHistory> histories = createShipmentHistories(
                shipment,
                routes
        );

        shipment.addItems(items);
        shipment.addHistories(histories);

        return shipment;
    }

    /**
     * 배송 상품 생성
     */
    private List<ShipmentItem> createShipmentItems(
            Shipment shipment,
            List<ShipmentCreateRequest.Item> items
    ) {
        return items.stream()
                .map(item -> ShipmentItem.create(
                        shipment,
                        item.productVariantId(),
                        item.quantity()
                ))
                .toList();
    }

    /**
     * 배송 이동 이력 생성
     */
    private List<ShipmentHistory> createShipmentHistories(
            Shipment shipment,
            List<ShortestPathResponse.RouteSegmentResponse> routes
    ) {

        // 허브 배송 담당자 배정
        List<UUID> hubManagers =
                deliveryManagerService.assignHubDeliveryManagers(routes.size());

        return IntStream.range(0, routes.size())
                .mapToObj(i -> {
                    var route = routes.get(i);

                    return ShipmentHistory.create(
                            shipment,
                            route.originHubId(),
                            route.sequence(),
                            route.destinationHubId(),
                            hubManagers.get(i),
                            ShipmentStatus.HUB_WAITING,
                            route.estimatedDuration().intValue(),
                            0,
                            0,
                            ""
                    );
                })
                .toList();
    }

    /**
     * 배송 주소 생성
     */
    private String buildAddress(ShipmentCreateRequest request) {
        return String.format(
                "%s %s",
                request.address(),
                request.addressDetail()
        );
    }
    //endregion

    @Transactional
    public void delete(String userId, UUID shipmentId){
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(ShipmentNotFoundException::new);

        shipment.delete(userId);
    }

    public ShipmentViewResponse getShipment(String userId, UUID shipmentId){
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(ShipmentNotFoundException::new);

        return ShipmentViewResponse.from(shipment);
    }
    
    public Page<ShipmentViewResponse> search(ShipmentSearchRequest request, Pageable pageable){
        return shipmentRepository.search(request, pageable)
                .map(ShipmentViewResponse::from);
    }

    private void validateUpdatePermission(List<Shipment> shipments, UUID managerId) {
        boolean hasPermission = shipments.stream()
                .allMatch(shipment -> shipment.isAssignedDeliveryManager(managerId));

        if (!hasPermission) {
            throw new ShipmentUpdateDeniedException();
        }
    }

}
