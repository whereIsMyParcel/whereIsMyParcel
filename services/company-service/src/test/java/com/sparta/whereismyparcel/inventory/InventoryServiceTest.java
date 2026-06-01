package com.sparta.whereismyparcel.inventory;

import com.sparta.whereismyparcel.inventory.application.service.InventoryService;
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
import com.sparta.whereismyparcel.product.domain.entity.Product;
import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;
import com.sparta.whereismyparcel.product.domain.repository.ProductVariantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;


    @InjectMocks
    private InventoryService inventoryService;

    private Product createProduct() {
        return Product.create(
                "테스트 상품",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 설명",
                15000

        );
    }

    @Test
    @DisplayName("재고 등록 성공 - 해당 허브에 동일한 배리언트 재고가 없다면 신규 재고가 정상 등록된다")
    void addStockSuccess() {
        // given
        UUID variantId = UUID.randomUUID();
        UUID actualHubId = UUID.randomUUID();
        AddInventoryRequest request = new AddInventoryRequest(variantId, 100, 10);


        Product product = createProduct();
        Product productSpy = spy(product);
        given(productSpy.getHubId()).willReturn(actualHubId);

        ProductVariant variantSpy = spy(ProductVariant.addVariant(productSpy, "SKU-I01", "재고 테스트 상품", 20000));
        given(variantSpy.getId()).willReturn(variantId);

        given(productVariantRepository.findById(variantId)).willReturn(Optional.of(variantSpy));

        given(inventoryRepository.findByHubIdAndProductVariant(actualHubId, variantSpy))
                .willReturn(Optional.empty());

        given(inventoryRepository.save(any(Inventory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        AddInventoryResponse response = inventoryService.addStock(request);

        // then
        assertThat(response).isNotNull();
        then(inventoryRepository).should(times(1)).save(any(Inventory.class));
    }

    @Test
    @DisplayName("재고 등록 실패 - 존재하지 않는 배리언트 ID 요청 시 ProductVariantNotFoundException이 터진다")
    void addStockFailVariantNotFound() {
        // given
        UUID wrongVariantId = UUID.randomUUID();
        AddInventoryRequest request = new AddInventoryRequest(wrongVariantId, 100, 10);

        given(productVariantRepository.findById(wrongVariantId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.addStock(request))
                .isInstanceOf(ProductVariantNotFoundException.class);
    }

    @Test
    @DisplayName("재고 등록 실패 - 해당 허브에 이미 동일 배리언트 재고가 존재하면 InventoryAlreadyExistsException이 터진다")
    void addStockFailInventoryAlreadyExists() {
        // given
        UUID variantId = UUID.randomUUID();
        UUID actualHubId = UUID.randomUUID();
        AddInventoryRequest request = new AddInventoryRequest(variantId, 100, 10);

        Product product = createProduct();
        Product productSpy = spy(product);
        given(productSpy.getHubId()).willReturn(actualHubId);

        ProductVariant variantSpy = spy(ProductVariant.addVariant(productSpy, "SKU-I02", "중복 재고 상품", 20000));


        given(productVariantRepository.findById(variantId)).willReturn(Optional.of(variantSpy));

        Inventory inventoryMock = mock(Inventory.class);
        given(inventoryRepository.findByHubIdAndProductVariant(actualHubId, variantSpy))
                .willReturn(Optional.of(inventoryMock));

        // when & then
        assertThatThrownBy(() -> inventoryService.addStock(request))
                .isInstanceOf(InventoryAlreadyExistsException.class);

        then(inventoryRepository).should(never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("재고 확인 성공 - 존재하는 배리언트와 허브에 재고가 등록되어 있다면 재고 정보를 정상 반환한다")
    void checkStockSuccess() {
        // given
        UUID variantId = UUID.randomUUID();
        UUID managedHubId = UUID.randomUUID();

        Product product = createProduct();
        Product productSpy = spy(product);
        given(productSpy.getHubId()).willReturn(managedHubId);

        ProductVariant variantSpy = spy(ProductVariant.addVariant(productSpy, "SKU-C01", "검증 상품", 30000));

        given(productVariantRepository.findById(variantId)).willReturn(Optional.of(variantSpy));

        Inventory inventoryMock = mock(Inventory.class);
        given(inventoryRepository.findByHubIdAndProductVariant(managedHubId, variantSpy))
                .willReturn(Optional.of(inventoryMock));

        // when
        InventoryCheckResponse response = inventoryService.checkStock(variantId);

        // then
        assertThat(response).isNotNull();
        then(inventoryRepository).should(times(1)).findByHubIdAndProductVariant(managedHubId, variantSpy);
    }

    @Test
    @DisplayName("재고 확인 실패 - 존재하지 않는 배리언트 ID 요청 시 ProductVariantNotFoundException이 터진다")
    void checkStockFailVariantNotFound() {
        // given
        UUID wrongVariantId = UUID.randomUUID();

        given(productVariantRepository.findById(wrongVariantId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.checkStock(wrongVariantId))
                .isInstanceOf(ProductVariantNotFoundException.class);
    }

    @Test
    @DisplayName("재고 확인 실패 - 해당 허브에 배리언트 재고 원장이 존재하지 않으면 InventoryNotFoundException이 터진다")
    void checkStockFailInventoryNotFound() {
        // given
        UUID variantId = UUID.randomUUID();
        UUID managedHubId = UUID.randomUUID();

        Product product = createProduct();
        Product productSpy = spy(product);
        given(productSpy.getHubId()).willReturn(managedHubId);

        ProductVariant variantSpy = spy(ProductVariant.addVariant(productSpy, "SKU-C02", "미등록 재고 상품", 30000));

        given(productVariantRepository.findById(variantId)).willReturn(Optional.of(variantSpy));

        given(inventoryRepository.findByHubIdAndProductVariant(managedHubId, variantSpy))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.checkStock(variantId))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    @DisplayName("재고 선점 성공 - 여러 아이템 요청 시 SKU 코드 순으로 정렬되어 안전하게 재고가 선점된다")
    void reserveOrderStockSuccess() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID fakeHubId = UUID.randomUUID();

        StockReservationRequest.Item itemB = new StockReservationRequest.Item(fakeHubId,"SKU-B", 2);
        StockReservationRequest.Item itemA = new StockReservationRequest.Item(fakeHubId,"SKU-A", 5);
        StockReservationRequest request = new StockReservationRequest(orderId, List.of(itemB, itemA));

        Product product = createProduct();

        ProductVariant variantA = spy(ProductVariant.addVariant(product, "SKU-A", "A 상품", 10000));
        UUID variantIdA = UUID.randomUUID();
        given(variantA.getId()).willReturn(variantIdA);

        Inventory inventorySpyA = spy(Inventory.addInventory(UUID.randomUUID(), variantA, 100, 10));
        given(inventorySpyA.getProductVariant()).willReturn(variantA);

        ProductVariant variantB = spy(ProductVariant.addVariant(product, "SKU-B", "B 상품", 20000));
        UUID variantIdB = UUID.randomUUID();
        given(variantB.getId()).willReturn(variantIdB);

        Inventory inventorySpyB = spy(Inventory.addInventory(UUID.randomUUID(), variantB, 100, 10));
        given(inventorySpyB.getProductVariant()).willReturn(variantB);

        given(productVariantRepository.findProductBySkuCode("SKU-A")).willReturn(Optional.of(variantA));
        given(productVariantRepository.findProductBySkuCode("SKU-B")).willReturn(Optional.of(variantB));

        given(inventoryRepository.findByProductVariant(variantA)).willReturn(Optional.of(inventorySpyA));
        given(inventoryRepository.findByProductVariant(variantB)).willReturn(Optional.of(inventorySpyB));

        // when
        List<StockReservationResponse> responses = inventoryService.reserveOrderStock(request);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).productVariantId()).isEqualTo(variantIdA);
        assertThat(responses.get(1).productVariantId()).isEqualTo(variantIdB);

        then(inventorySpyA).should(times(1)).addReservedStock(5);
        then(inventorySpyB).should(times(1)).addReservedStock(2);
    }

    @Test
    @DisplayName("재고 선점 실패 - 요청한 SKU 코드를 가진 배리언트가 없으면 ProductVariantNotFoundException이 터진다")
    void reserveOrderStockFailVariantNotFound() {
        // given
        StockReservationRequest.Item item = new StockReservationRequest.Item(UUID.randomUUID(),"WRONG-SKU", 1);
        StockReservationRequest request = new StockReservationRequest(UUID.randomUUID(), List.of(item));

        given(productVariantRepository.findProductBySkuCode("WRONG-SKU")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.reserveOrderStock(request))
                .isInstanceOf(ProductVariantNotFoundException.class);

        then(inventoryRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("재고 선점 실패 - 배리언트는 존재하지만 등록된 재고 원장이 없으면 InventoryNotFoundException이 터진다")
    void reserveOrderStockFailInventoryNotFound() {
        // given
        StockReservationRequest.Item item = new StockReservationRequest.Item(UUID.randomUUID(), "SKU-EXIST", 3);
        StockReservationRequest request = new StockReservationRequest(UUID.randomUUID(), List.of(item));

        Product product = createProduct();
        ProductVariant variant = spy(ProductVariant.addVariant(product, "SKU-EXIST", "존재하는 상품", 10000));

        given(productVariantRepository.findProductBySkuCode("SKU-EXIST")).willReturn(Optional.of(variant));
        given(inventoryRepository.findByProductVariant(variant)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.reserveOrderStock(request))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    @DisplayName("출고 확정 성공 - 존재하는 배리언트의 재고가 있다면 정상 출고 확정되고 자동 보충 로직이 실행된다")
    void confirmDeliveryLaunchSuccess() {
        // given
        UUID variantId = UUID.randomUUID();
        StockConfirmRequest request = new StockConfirmRequest(variantId, 10);

        Product product = createProduct();
        ProductVariant variantSpy = spy(ProductVariant.addVariant(product, "SKU-D01", "출고 상품", 15000));

        Inventory inventorySpy = spy(Inventory.addInventory(UUID.randomUUID(), variantSpy, 100, 10));

        inventorySpy.addReservedStock(10);

        // 레포지토리 가스라이팅
        given(productVariantRepository.findById(variantId)).willReturn(Optional.of(variantSpy));
        given(inventoryRepository.findByProductVariant(variantSpy)).willReturn(Optional.of(inventorySpy));

        // when
        inventoryService.confirmDeliveryLaunch(request);

        // then
        then(inventorySpy).should(times(1)).confirmShipment(10);
        then(inventorySpy).should(times(1)).autoIncreaseQuantity();
    }

    @Test
    @DisplayName("출고 확정 실패 - 존재하지 않는 배리언트 ID 요청 시 ProductVariantNotFoundException이 터진다")
    void confirmDeliveryLaunchFailVariantNotFound() {
        // given
        UUID wrongVariantId = UUID.randomUUID();
        StockConfirmRequest request = new StockConfirmRequest(wrongVariantId, 10);

        given(productVariantRepository.findById(wrongVariantId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.confirmDeliveryLaunch(request))
                .isInstanceOf(ProductVariantNotFoundException.class);

        then(inventoryRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("출고 확정 실패 - 배리언트는 존재하지만 등록된 재고 원장이 없으면 InventoryNotFoundException이 터진다")
    void confirmDeliveryLaunchFailInventoryNotFound() {
        // given
        UUID variantId = UUID.randomUUID();
        StockConfirmRequest request = new StockConfirmRequest(variantId, 10);

        Product product = createProduct();
        ProductVariant variantSpy = spy(ProductVariant.addVariant(product, "SKU-D02", "재고 없는 상품", 15000));

        given(productVariantRepository.findById(variantId)).willReturn(Optional.of(variantSpy));
        given(inventoryRepository.findByProductVariant(variantSpy)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.confirmDeliveryLaunch(request))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    @DisplayName("재고 복구 성공 - 주문 취소 요청 시 SKU 코드 순으로 정렬되어 안전하게 예약 재고가 차감 복구된다")
    void cancelOrderReservationSuccess() {
        // given
        UUID orderId = UUID.randomUUID();

        StockCancelRequest.Item itemB = new StockCancelRequest.Item("SKU-B", 2);
        StockCancelRequest.Item itemA = new StockCancelRequest.Item("SKU-A", 5);
        StockCancelRequest request = new StockCancelRequest(orderId, List.of(itemB, itemA));

        Product product = createProduct();

        ProductVariant variantA = spy(ProductVariant.addVariant(product, "SKU-A", "A 상품", 10000));
        Inventory inventorySpyA = spy(Inventory.addInventory(UUID.randomUUID(), variantA, 100, 10));
        inventorySpyA.addReservedStock(5);

        ProductVariant variantB = spy(ProductVariant.addVariant(product, "SKU-B", "B 상품", 20000));
        Inventory inventorySpyB = spy(Inventory.addInventory(UUID.randomUUID(), variantB, 100, 10));
        inventorySpyB.addReservedStock(2);

        given(productVariantRepository.findProductBySkuCode("SKU-A")).willReturn(Optional.of(variantA));
        given(productVariantRepository.findProductBySkuCode("SKU-B")).willReturn(Optional.of(variantB));

        given(inventoryRepository.findByProductVariant(variantA)).willReturn(Optional.of(inventorySpyA));
        given(inventoryRepository.findByProductVariant(variantB)).willReturn(Optional.of(inventorySpyB));

        // when
        inventoryService.cancelOrderReservation(request);

        // then
        then(inventorySpyA).should(times(1)).cancelReservation(5);
        then(inventorySpyB).should(times(1)).cancelReservation(2);
    }

    @Test
    @DisplayName("재고 복구 실패 - 요청한 SKU 코드를 가진 배리언트가 없으면 ProductVariantNotFoundException이 터진다")
    void cancelOrderReservationFailVariantNotFound() {
        // given
        UUID orderId = UUID.randomUUID();

        StockCancelRequest.Item item = new StockCancelRequest.Item("WRONG-SKU", 1);
        StockCancelRequest request = new StockCancelRequest(orderId, List.of(item));

        given(productVariantRepository.findProductBySkuCode("WRONG-SKU")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.cancelOrderReservation(request))
                .isInstanceOf(ProductVariantNotFoundException.class);

        then(inventoryRepository).shouldHaveNoInteractions();
    }
}
