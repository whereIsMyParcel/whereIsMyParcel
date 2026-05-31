package com.sparta.whereismyparcel.inventory.application.service;

import com.sparta.whereismyparcel.inventory.domain.entity.Inventory;
import com.sparta.whereismyparcel.inventory.domain.exception.InventoryAlreadyExistsException;
import com.sparta.whereismyparcel.inventory.domain.exception.InventoryNotFoundException;
import com.sparta.whereismyparcel.inventory.domain.exception.ProductVariantNotFoundException;
import com.sparta.whereismyparcel.inventory.domain.repository.InventoryRepository;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.AddInventoryRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockConfirmRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockReservationRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.AddInventoryResponse;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.InventoryCheckResponse;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.StockReservationResponse;
import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import com.sparta.whereismyparcel.product.domain.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository productVariantRepository;

    // 재고 등록
    @Transactional
    public AddInventoryResponse addStock(AddInventoryRequest request) {
        ProductVariant productVariant = productVariantRepository.findById(request.productVariantId())
                .orElseThrow(ProductVariantNotFoundException::new);

        // 상품에 링크된 진짜 허브 ID 추출
        UUID actualHubId = productVariant.getProduct().getHubId();


        inventoryRepository.findByHubIdAndProductVariant(actualHubId, productVariant)
                .ifPresent(inventory -> {
                    throw new InventoryAlreadyExistsException();
                });

        Inventory inventory = Inventory.addInventory(
                actualHubId,
                productVariant,
                request.quantity(),
                request.safetyStockQuantity()
        );
        inventoryRepository.save(inventory);
        return AddInventoryResponse.from(inventory);
    }

    // 재고 확인
    public InventoryCheckResponse checkStock(UUID productVariantId) {
        ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                .orElseThrow(ProductVariantNotFoundException::new);

        UUID managedHubId = productVariant.getProduct().getHubId();

        Inventory inventory = inventoryRepository.findByHubIdAndProductVariant(managedHubId, productVariant)
                .orElseThrow(InventoryNotFoundException::new);

        return InventoryCheckResponse.from(inventory);
    }

    /**
     * 주문 생성 시 수량 예약 선점 (Order ➡︎ Inventory)
     */
    @Retryable(
            retryFor = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    @Transactional
    public List<StockReservationResponse> reserveOrderStock(StockReservationRequest request) {
        List<StockReservationRequest.Item> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(StockReservationRequest.Item::skuCode))
                .toList();

        return sortedItems.stream()
                .map(item -> {
                    ProductVariant variant = productVariantRepository.findProductBySkuCode(item.skuCode())
                            .orElseThrow(ProductVariantNotFoundException::new);

                    Inventory inventory = inventoryRepository.findByProductVariant(variant)
                            .orElseThrow(InventoryNotFoundException::new);

                    inventory.addReservedStock(item.quantity());

                    return new StockReservationResponse(
                            inventory.getProductVariant().getId(),
                            item.quantity());
                })
                .toList();
    }

    /**
     * 배송 시작 시 출고 확정 및 예약 해제 (Delivery ➡︎ Inventory)
     */
    @Transactional
    public void confirmDeliveryLaunch(StockConfirmRequest request) {
        ProductVariant productVariant = productVariantRepository.findById(request.productVariantId())
                .orElseThrow(ProductVariantNotFoundException::new);

        Inventory inventory = inventoryRepository.findByProductVariant(productVariant)
                .orElseThrow(InventoryNotFoundException::new);

        inventory.confirmShipment(request.quantity());

        inventory.autoIncreaseQuantity();
    }

    /**
     * 예외 시나리오: 배송 전 주문 취소 시 재고 복구 (Order/Delivery ➡︎ Inventory)
     */
    @Transactional
    public void cancelOrderReservation(StockCancelRequest request) {
        List<StockCancelRequest.Item> sortedItems = request.items().stream()
                .sorted(java.util.Comparator.comparing(StockCancelRequest.Item::skuCode))
                .toList();

        sortedItems.forEach(item -> {
            ProductVariant productVariant = productVariantRepository.findProductBySkuCode(item.skuCode())
                    .orElseThrow(ProductVariantNotFoundException::new);

            Inventory inventory = inventoryRepository.findByProductVariant(productVariant)
                    .orElseThrow(InventoryNotFoundException::new);

            inventory.cancelReservation(item.quantity());
        });
    }
}
