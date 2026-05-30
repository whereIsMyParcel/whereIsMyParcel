package com.sparta.whereismyparcel.order.application.saga;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.service.OrderCreationStateService;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.ShipmentCreateRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateSaga {

    private final CompanyFeignClient companyFeignClient;
    private final ShipmentFeignClient shipmentFeignClient;
    private final OrderCreationStateService orderCreationStateService;

    public void execute(OrderCreateSagaContext context) {
        try {
            reserveStock(context);
        } catch (Exception e) {
            log.error("[Saga] 재고 예약 실패. orderId={}", context.getOrderId(), e);
            orderCreationStateService.markFailed(context.getOrderId());
            throw new SagaFailedException();
        }

        try {
            createShipments(context);
            orderCreationStateService.markConfirmed(context.getOrderId());
        } catch (Exception e) {
            log.error("[Saga] 배송 생성 실패. orderId={}", context.getOrderId(), e);
            handleShipmentCreationFailure(context);
        }
    }

    private void reserveStock(OrderCreateSagaContext context) {
        var request = new StockReservationRequest(
                context.getOrderId(),
                context.getItems().stream()
                        .map(i -> new StockReservationRequest.Item(i.skuCode(), i.quantity()))
                        .toList()
        );
        var reservations = resolveSagaResponse(
                companyFeignClient.reserveStock(context.getUserId(), request)
        );
        context.applyReservation(reservations.stream()
                .map(r -> new OrderCreateSagaContext.StockReservation(
                        r.productVariantId(),
                        findSkuCode(context, r.productVariantId()),
                        r.reservedQuantity()
                ))
                .toList());
        orderCreationStateService.markStockReserved(context.getOrderId());
    }

    private void createShipments(OrderCreateSagaContext context) {
        var request = new ShipmentCreateRequest(
                context.getOrderId(),
                context.getRecipientName(),
                context.getRecipientPhone(),
                context.getZipCode(),
                context.getAddress(),
                context.getAddressDetail(),
                context.getItems().stream()
                        .map(i -> new ShipmentCreateRequest.Item(i.productVariantId(), i.quantity()))
                        .toList()
        );
        var shipmentIds = resolveSagaResponse(
                shipmentFeignClient.createShipments(context.getUserId(), request)
        ).stream()
                .map(response -> response.shipmentId())
                .toList();
        context.applyShipmentIds(shipmentIds);
    }

    private void handleShipmentCreationFailure(OrderCreateSagaContext context) {
        try {
            compensateStockReservation(context);
            orderCreationStateService.markFailed(context.getOrderId());
            throw new SagaFailedException();
        } catch (SagaCompensationFailedException e) {
            orderCreationStateService.markCompensationFailed(context.getOrderId());
            throw e;
        }
    }

    private void compensateStockReservation(OrderCreateSagaContext context) {
        try {
            var request = new StockCancelRequest(
                    context.getOrderId(),
                    context.getReservations().stream()
                            .map(r -> new StockCancelRequest.Item(r.skuCode(), r.quantity()))
                            .toList()
            );
            ensureCompensationSuccess(companyFeignClient.cancelReservation(context.getUserId(), request));
        } catch (Exception e) {
            log.error("[Saga] 재고 원복 실패. orderId={}", context.getOrderId(), e);
            throw new SagaCompensationFailedException();
        }
    }

    private <T> T resolveSagaResponse(ApiResponse<T> response) {
        if (response == null || !response.success() || response.data() == null) {
            throw new SagaFailedException();
        }
        return response.data();
    }

    private void ensureCompensationSuccess(ApiResponse<Void> response) {
        if (response == null || !response.success()) {
            throw new SagaCompensationFailedException();
        }
    }

    private String findSkuCode(OrderCreateSagaContext context, UUID productVariantId) {
        return context.getItems().stream()
                .filter(item -> item.productVariantId().equals(productVariantId))
                .map(OrderCreateSagaContext.OrderItemInfo::skuCode)
                .findFirst()
                .orElseThrow(SagaFailedException::new);
    }
}
