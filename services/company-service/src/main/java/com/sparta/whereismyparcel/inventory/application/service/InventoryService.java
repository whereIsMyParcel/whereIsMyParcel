package com.sparta.whereismyparcel.inventory.application.service;

import com.sparta.whereismyparcel.common.dto.HubValidateRequest;
import com.sparta.whereismyparcel.common.infrastructure.client.HubFeignClient;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.inventory.domain.entity.Inventory;
import com.sparta.whereismyparcel.inventory.domain.repository.InventoryRepository;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.AddInventoryRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.InventoryStockRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockReservationRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.AddInventoryResponse;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.StockReservationResponse;
import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import com.sparta.whereismyparcel.product.domain.repository.ProductVariantRepository;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.InventoryCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final HubFeignClient hubFeignClient;

    @Transactional
    public AddInventoryResponse addStock(AddInventoryRequest request) {
        // 허브 존재 확인 feign 요청
        ApiResponse<Void> hubCheck = hubFeignClient.validateHub(new HubValidateRequest(request.hubId()));
        if (hubCheck == null || !hubCheck.success()) {
            throw new IllegalArgumentException("존재하지 않는 허브입니다");
        }

        ProductVariant productVariant = productVariantRepository.findById(request.productVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));

        inventoryRepository.findByHubIdAndProductVariant(request.hubId(), productVariant)
                .ifPresent(inventory -> {
                    throw new IllegalArgumentException("Inventory already exists");
                });

        Inventory inventory = Inventory.addInventory(
                request.hubId(),
                productVariant,
                request.quantity(),
                request.safetyStockQuantity()
        );
        inventoryRepository.save(inventory);
        return AddInventoryResponse.from(inventory);
    }

    /**
     * ③ 주문 생성 시 수량 예약 선점 (Order ➡️ Inventory)
     */
    @Transactional
    public List<StockReservationResponse> reserveOrderStock(StockReservationRequest request) {
        return request.items().stream()
                .map(item -> {
                    ProductVariant variant = productVariantRepository.findProductBySkuCode(item.skuCode())
                            .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));

                    Inventory inventory = inventoryRepository.findByHubIdAndProductVariant(item.hubId(),variant)
                            .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

                    inventory.addReservedStock(item.quantity());
                    return new StockReservationResponse(
                            inventory.getProductVariant().getId(),
                            item.quantity());
                })
                .toList();
    }

    /**
     * ④ 배송 시작 시 출고 확정 및 예약 해제 (Delivery ➡️ Inventory)
     */
    @Transactional
    public void confirmDeliveryLaunch(InventoryStockRequest request) {
        ProductVariant productVariant = productVariantRepository.findById(request.productVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));

        Inventory inventory = inventoryRepository.findByHubIdAndProductVariant(request.hubId(), productVariant)
                .orElseThrow(() -> new IllegalArgumentException("재고 정보를 찾을 수 없습니다."));

        inventory.confirmShipment(request.quantity());

        inventory.autoIncreaseQuantity();
    }

    /**
     * 🔄 예외 시나리오: 배송 전 주문 취소 시 재고 복구 (Order/Delivery ➡️ Inventory)
     */
    @Transactional
    public void cancelOrderReservation(StockCancelRequest request) {
        request.items().forEach(item -> {
            ProductVariant variant = productVariantRepository.findProductBySkuCode(item.skuCode())
                    .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));

            Inventory inventory = inventoryRepository.findByHubIdAndProductVariant(item.hubId(), variant)
                    .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
            inventory.cancelReservation(item.quantity());
        });
    }
}
