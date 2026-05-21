package com.sparta.whereismyparcel.order.application.saga;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.ShipmentCreateRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockReservationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreateSaga {

    private final CompanyFeignClient companyFeignClient;
    private final ShipmentFeignClient shipmentFeignClient;

    public void execute(Order order, OrderCreateSagaContext context) {

        try {
            var request = new StockReservationRequest(
                    context.getOrderId(),
                    context.getItems().stream()
                            .map(i -> new StockReservationRequest.Item(i.productVariantId(), i.quantity()))
                            .toList()
            );
            var reservations = companyFeignClient.reserveStock(context.getUserId(), request);
            context.applyReservation(reservations.stream()
                    .map(r -> new OrderCreateSagaContext.StockReservation(r.skuId(), null, r.reservedQuantity()))
                    .toList());
        } catch (Exception e) {
            order.fail();
            throw new SagaFailedException();
        }

        try {
            var request = new ShipmentCreateRequest(
                    context.getOrderId(),
                    order.getRecipientName(),
                    order.getRecipientPhone(),
                    order.getZipCode(),
                    order.getAddress(),
                    order.getAddressDetail(),
                    context.getItems().stream()
                            .map(i -> new ShipmentCreateRequest.Item(i.productVariantId(), i.quantity()))
                            .toList()
            );
            var shipmentIds = shipmentFeignClient.createShipments(context.getUserId(), request);
            context.applyShipmentIds(shipmentIds);
        } catch (Exception e) {
            order.fail();
            compensateStockReservation(context);
            throw new SagaFailedException();
        }

        order.confirm();
    }

    private void compensateStockReservation(OrderCreateSagaContext context) {
        try {
            var request = new StockCancelRequest(
                    context.getOrderId(),
                    context.getReservations().stream()
                            .map(r -> new StockCancelRequest.Item(r.skuId(), r.quantity()))
                            .toList()
            );
            companyFeignClient.cancelReservation(context.getUserId(), request);
        } catch (Exception e) {
            throw new SagaCompensationFailedException();
        }
    }
}
