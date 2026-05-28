package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSaga;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSagaContext;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderItemsException;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderSearchDateRangeException;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderStatusException;
import com.sparta.whereismyparcel.order.domain.exception.OrderNotFoundException;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import com.sparta.whereismyparcel.order.infrastructure.client.AiSlackFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.AiAnalysisRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.ShipmentCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderDispatchDeadlineUpdateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderUpdateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderAiContextResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCancelResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderDetailResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderDispatchDeadlineUpdateResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderListResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private static final String MASTER_ROLE = "MASTER";

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Duration ORDER_CANCEL_LIMIT = Duration.ofMinutes(5);

    private final OrderRepository orderRepository;
    private final CompanyFeignClient companyFeignClient;
    private final ShipmentFeignClient shipmentFeignClient;
    private final AiSlackFeignClient aiSlackFeignClient;
    private final OrderCreateSaga orderCreateSaga;

    @Transactional
    public OrderCreateResponse createOrder(String userId, OrderCreateRequest request) {
        List<UUID> productVariantIds = request.items().stream()
                .map(OrderCreateRequest.OrderItemCreateRequest::productVariantId)
                .toList();
        List<SkuValidationResponse> validations = resolveSkuValidation(
                companyFeignClient.validateProducts(userId, productVariantIds)
        );
        Map<UUID, SkuValidationResponse> validationMap = validations.stream()
                .collect(Collectors.toMap(
                        SkuValidationResponse::variantId,
                        Function.identity()
                ));

        List<OrderItem> orderItems = request.items().stream()
                .map(i -> {
                    SkuValidationResponse skuInfo = validationMap.get(i.productVariantId());
                    if (skuInfo == null) {
                        throw new InvalidOrderItemsException();
                    }
                    return OrderItem.create(
                            i.productVariantId(),
                            skuInfo.skuCode(),
                            skuInfo.variantName(),
                            skuInfo.variantName(),
                            skuInfo.variantPrice().longValue(),
                            i.quantity()
                    );
                })
                .toList();

        Order order = Order.create(
                request.companyMemberId(),
                generateOrderNumber(),
                request.recipientName(),
                request.recipientPhone(),
                request.zipCode(),
                request.address(),
                request.addressDetail(),
                request.requestMemo(),
                request.requestedDeliveryAt(),
                userId,
                orderItems
        );

        orderRepository.save(order);

        List<OrderCreateSagaContext.OrderItemInfo> itemInfos = orderItems.stream()
                .map(item -> new OrderCreateSagaContext.OrderItemInfo(
                        item.getProductVariantId(),
                        item.getSkuCode(),
                        item.getQuantity()
                ))
                .toList();
        OrderCreateSagaContext context = new OrderCreateSagaContext(
                order.getOrderId(), userId, itemInfos);

        try {
            orderCreateSaga.execute(order, context);
        } catch (SagaCompensationFailedException e) {
            log.error("[OrderService] 보상 실패. orderId={}", order.getOrderId(), e);
        } catch (SagaFailedException e) {
            log.error("[OrderService] Saga 실패. orderId={}", order.getOrderId(), e);
        } finally {
            orderRepository.save(order);
        }

        if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
            registerAiAnalysisAfterCommit(userId, order.getOrderId());
        }

        return OrderCreateResponse.from(order);
    }

    public Page<OrderListResponse> getOrders(
            String userId,
            String role,
            OrderStatus status,
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        validateSearchDateRange(startDate, endDate);

        return orderRepository.findAll(
                orderSearchSpecification(
                        userId,
                        isMaster(role),
                        status,
                        normalizeKeyword(keyword),
                        startDate,
                        endDate
                ),
                pageable
        ).map(OrderListResponse::from);
    }

    private Specification<Order> orderSearchSpecification(
            String userId,
            boolean isMaster,
            OrderStatus status,
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isMaster) {
                predicates.add(cb.equal(root.get("orderedBy"), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("orderStatus"), status));
            }

            if (keyword != null) {
                String lowerKeywordPattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                String upperKeywordPattern = "%" + keyword.toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(root.get("orderNumber"), upperKeywordPattern),
                        cb.like(cb.lower(root.get("recipientName")), lowerKeywordPattern),
                        cb.like(root.get("recipientPhone"), "%" + keyword + "%")
                ));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderedAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderedAt"), endDate));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    public OrderDetailResponse getOrder(String userId, String role, UUID orderId) {
        Order order = orderRepository.findDetailByOrderId(orderId, userId, isMaster(role))
                .orElseThrow(OrderNotFoundException::new);

        return OrderDetailResponse.from(order);
    }

    public OrderAiContextResponse getOrderAiContext(UUID orderId) {
        Order order = orderRepository.findWithOrderItemsByOrderId(orderId)
                .orElseThrow(OrderNotFoundException::new);

        return OrderAiContextResponse.from(order);
    }

    @Transactional
    public OrderDispatchDeadlineUpdateResponse updateFinalDispatchDeadline(
            UUID orderId,
            OrderDispatchDeadlineUpdateRequest request
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        order.updateFinalDispatchDeadline(request.finalDispatchDeadline());

        return OrderDispatchDeadlineUpdateResponse.from(order);
    }

    @Transactional
    public OrderUpdateResponse updateOrder(
            String userId,
            String role,
            UUID orderId,
            OrderUpdateRequest request
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!isMaster(role)) {
            validateOrderOwner(order, userId);
        }

        order.updateRequestInfo(request.requestMemo(), request.requestedDeliveryAt());

        return OrderUpdateResponse.from(order);
    }

    @Transactional
    public OrderCancelResponse cancelOrder(String userId, String role, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!isMaster(role)) {
            validateOrderOwner(order, userId);
        }

        if (!isCancelableStatus(order.getOrderStatus())) {
            throw new InvalidOrderStatusException();
        }

        order.validateCancelableTime(LocalDateTime.now(), ORDER_CANCEL_LIMIT);

        String ownerId = order.getOrderedBy();

        switch (order.getOrderStatus()) {
            case PENDING -> order.cancel();
            case STOCK_RESERVED -> {
                cancelStockReservation(ownerId, order);
                order.cancel();
            }
            case CONFIRMED -> {
                cancelShipments(ownerId, order);
                cancelStockReservation(ownerId, order);
                order.cancel();
            }
            default -> throw new InvalidOrderStatusException();
        }

        return OrderCancelResponse.from(order);
    }

    @Transactional
    public void completeOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            return;
        }

        order.complete();
    }

    @Transactional
    public void deleteOrder(String userId, String role, UUID orderId) {
        if (!isMaster(role)) {
            throw new OrderNotFoundException();
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.isDeletable()) {
            throw new InvalidOrderStatusException();
        }

        order.delete(userId);
    }

    private void validateOrderOwner(Order order, String userId) {
        if (!order.getOrderedBy().equals(userId)) {
            throw new OrderNotFoundException();
        }
    }

    private boolean isCancelableStatus(OrderStatus orderStatus) {
        return orderStatus == OrderStatus.PENDING
                || orderStatus == OrderStatus.STOCK_RESERVED
                || orderStatus == OrderStatus.CONFIRMED;
    }

    private boolean isMaster(String role) {
        return MASTER_ROLE.equals(role);
    }

    private void validateSearchDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidOrderSearchDateRangeException();
        }
    }

    private void cancelStockReservation(String userId, Order order) {
        StockCancelRequest request = new StockCancelRequest(
                order.getOrderId(),
                order.getOrderItems().stream()
                        .map(item -> new StockCancelRequest.Item(
                                item.getSkuCode(),
                                item.getQuantity()
                        ))
                        .toList()
        );

        ApiResponse<Void> response = companyFeignClient.cancelReservation(userId, request);
        ensureCompensationSuccess(response);
    }

    private void cancelShipments(String userId, Order order) {
        ShipmentCancelRequest request = new ShipmentCancelRequest(order.getOrderId());
        ApiResponse<Void> response = shipmentFeignClient.cancelShipments(userId, request);
        ensureCompensationSuccess(response);
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(ORDER_NUMBER_DATE_FORMAT);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return "ORD-" + date + "-" + suffix;
    }

    private List<SkuValidationResponse> resolveSkuValidation(ApiResponse<List<SkuValidationResponse>> response) {
        if (response == null || !response.success() || response.data() == null || response.data().isEmpty()) {
            throw new InvalidOrderItemsException();
        }
        return response.data();
    }

    private void ensureCompensationSuccess(ApiResponse<Void> response) {
        if (response == null || !response.success()) {
            throw new SagaCompensationFailedException();
        }
    }

    private void registerAiAnalysisAfterCommit(String userId, UUID orderId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            requestAiAnalysis(userId, orderId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                requestAiAnalysis(userId, orderId);
            }
        });
    }

    private void requestAiAnalysis(String userId, UUID orderId) {
        try {
            ApiResponse<UUID> response = aiSlackFeignClient.createAiAnalysisRequest(
                    userId,
                    new AiAnalysisRequest(orderId)
            );

            if (response == null || !response.success()) {
                log.warn(
                        "[OrderService] AI 분석 요청 실패 응답. orderId={}, errorCode={}, message={}",
                        orderId,
                        response == null ? null : response.errorCode(),
                        response == null ? null : response.message()
                );
                return;
            }

            log.info("[OrderService] AI 분석 요청 완료. orderId={}, aiMessageId={}", orderId, response.data());
        } catch (Exception e) {
            log.warn("[OrderService] AI 분석 요청 예외 발생. orderId={}", orderId, e);
        }
    }
}
