package com.sparta.whereismyparcel.product;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

import com.sparta.whereismyparcel.product.application.service.ProductService;
import com.sparta.whereismyparcel.product.domain.entity.*;
import com.sparta.whereismyparcel.product.domain.exception.ProductNotFoundException;
import com.sparta.whereismyparcel.product.domain.exception.ProductOptionValueNotFoundException;
import com.sparta.whereismyparcel.product.domain.exception.UnsupportedProductStatus;
import com.sparta.whereismyparcel.product.domain.repository.*;
import com.sparta.whereismyparcel.product.presentation.dto.request.*;
import com.sparta.whereismyparcel.product.presentation.dto.response.*;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    // 1. 서비스가 의존하는 4개의 레포지토리를 전부 Mock(가짜)으로 선언
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private ProductOptionValueRepository productOptionValueRepository;

    @InjectMocks
    private ProductService productService;

    @NonNull
    private static ProductRegisterRequest getProductRegisterRequest(UUID companyId, UUID hubId) {
        OptionValueRegisterRequest valBlack = new OptionValueRegisterRequest("블랙", 0);
        OptionValueRegisterRequest valWhite = new OptionValueRegisterRequest("화이트", 0);
        OptionRegisterRequest optColor = new OptionRegisterRequest("색상", List.of(valBlack, valWhite));

        OptionValueRegisterRequest val260 = new OptionValueRegisterRequest("260", 5000);
        OptionRegisterRequest optSize = new OptionRegisterRequest("사이즈", List.of(val260));

        ProductRegisterRequest request = new ProductRegisterRequest(
                "나이키 에어포스",
                companyId,
                hubId,
                "국민 스니커즈",
                129000,
                List.of(optColor, optSize)
        );
        return request;
    }

    // 옵션없는 상품
    private static ProductRegisterRequest getProductRegisterRequestWithoutOptions(UUID companyId, UUID hubId) {
        return new ProductRegisterRequest(
                "나이키 에어포스",
                companyId,
                hubId,
                "옵션 없는 국민 스니커즈",
                129000,
                List.of()
        );
    }

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
    @DisplayName("상품과 여러 옵션들을 입력하면 데카르트 곱 연산을 통해 조합형 상품(Variant)들이 정상 등록된다")
    void registerProduct_success_with_options() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        ProductRegisterRequest request = getProductRegisterRequest(companyId, hubId);

        Product mockProduct = Product.create(request.name(), request.companyId(), request.hubId(), request.description(), request.price());

        Product spyProduct = spy(mockProduct);
        given(spyProduct.getId()).willReturn(productId);

        ProductOption mockColorOpt = ProductOption.addOption(spyProduct, "색상");
        ProductOption mockSizeOpt = ProductOption.addOption(spyProduct, "사이즈");

        ProductOptionValue mockBlack = ProductOptionValue.addOptionValue(mockColorOpt, "블랙", 0);
        ProductOptionValue mockWhite = ProductOptionValue.addOptionValue(mockColorOpt, "화이트", 0);
        ProductOptionValue mock260 = ProductOptionValue.addOptionValue(mockSizeOpt, "260", 5000);

        given(productRepository.save(any(Product.class))).willReturn(spyProduct);

        given(productOptionRepository.save(any(ProductOption.class))).willReturn(mockColorOpt, mockSizeOpt);
        given(productOptionValueRepository.save(any(ProductOptionValue.class))).willReturn(mockBlack, mockWhite, mock260);

        ProductVariant mockVariant1 = ProductVariant.addVariant(spyProduct, "SKU-MOCK-001", "나이키 에어포스 (블랙 / 260)", 134000);
        ProductVariant mockVariant2 = ProductVariant.addVariant(spyProduct, "SKU-MOCK-002", "나이키 에어포스 (화이트 / 260)", 134000);
        given(productVariantRepository.save(any(ProductVariant.class))).willReturn(mockVariant1, mockVariant2);

        // when
        ProductResponse response = productService.registerProduct(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("나이키 에어포스");
        assertThat(response.variants()).hasSize(2);

        assertThat(response.variants().get(0).variantName()).contains("블랙 / 260");
        assertThat(response.variants().get(1).variantName()).contains("화이트 / 260");

        verify(productRepository, times(1)).save(any(Product.class));
        verify(productOptionRepository, times(2)).save(any(ProductOption.class));
        verify(productOptionValueRepository, times(3)).save(any(ProductOptionValue.class));
        verify(productVariantRepository, times(2)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("옵션이 없는 상품은 카티스티안 알고리즘을 타지 않고 그대로 반환된다")
    void registerProduct_success_without_options() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        ProductRegisterRequest request = getProductRegisterRequestWithoutOptions(companyId, hubId);

        Product mockProduct = Product.create(request.name(), request.companyId(), request.hubId(), request.description(), request.price());

        Product spyProduct = spy(mockProduct);
        given(spyProduct.getId()).willReturn(productId);

        given(productRepository.save(any(Product.class))).willReturn(spyProduct);

        ProductVariant mockDefaultVariant = ProductVariant.addVariant(
                spyProduct,
                "SKU-DEFAULT-001",
                spyProduct.getName(),
                spyProduct.getPrice()
        );
        given(productVariantRepository.save(any(ProductVariant.class))).willReturn(mockDefaultVariant);

        // when
        ProductResponse response = productService.registerProduct(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("나이키 에어포스");

        assertThat(response.variants()).hasSize(1);
        assertThat(response.variants().get(0).variantName()).isEqualTo("나이키 에어포스");

        verify(productRepository, times(1)).save(any(Product.class));
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));

        verify(productOptionRepository, times(0)).save(any(ProductOption.class));
        verify(productOptionValueRepository, times(0)).save(any(ProductOptionValue.class));
    }

    @Test
    @DisplayName("상품 수정 성공 - 존재하는 상품이라면 상세 정보를 수정하고 소속된 모든 배리언트를 동기화한다")
    void updateProductSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        ProductUpdateRequest request = new ProductUpdateRequest("새로운 상품명", "새로운 설명", 25000);

        Product product = createProduct();

        ProductVariant variantSpy = spy(ProductVariant.addVariant(product, "SKU-100", "기본 상품명", 26000 ));
        product.getVariants().add(variantSpy);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        ProductUpdateResponse response = productService.updateProduct(productId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(product.getName()).isEqualTo("새로운 상품명");
        assertThat(product.getPrice()).isEqualTo(25000);

        then(variantSpy).should(times(1)).syncVariants();
    }

    @Test
    @DisplayName("상품 수정 실패 - 존재하지 않는 상품 ID면 ProductNotFoundException이 터진다")
    void updateProductFailProductNotFound() {
        // given
        UUID productId = UUID.randomUUID();
        ProductUpdateRequest request = new ProductUpdateRequest("새 이름", "새 설명", 10000);

        // 레포지토리가 빈 Optional을 뱉는 상황 모킹
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("옵션값 수정 성공 - 존재하는 상품과 옵션값이라면 상세 내용을 수정하고, 해당 옵션값을 가진 배리언트만 동기화한다")
    void updateOptionValueSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        OptionValueUpdateRequest request = new OptionValueUpdateRequest("블랙", 2000);

        Product product = createProduct();

        ProductOption option = ProductOption.addOption(product, "색상");

        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "화이트", 0));

        given(targetValue.getId()).willReturn(optionValueId);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        ProductVariant matchedVariantSpy = spy(ProductVariant.addVariant(product, "SKU-M01", "블랙 옵션", 10000));
        ProductVariant unmatchedVariantSpy = spy(ProductVariant.addVariant(product, "SKU-U01", "엉뚱한 옵션", 10000));

        product.getVariants().clear();
        product.getVariants().add(matchedVariantSpy);
        product.getVariants().add(unmatchedVariantSpy);

        given(matchedVariantSpy.containsOptionValue(targetValue)).willReturn(true);
        given(unmatchedVariantSpy.containsOptionValue(targetValue)).willReturn(false);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        List<VariantResponse> responses = productService.updateOptionValue(productId, optionValueId, request);

        // then
        assertThat(responses).hasSize(2);
        assertThat(targetValue.getValue()).isEqualTo("블랙");

        then(matchedVariantSpy).should(times(1)).syncVariants();
        then(unmatchedVariantSpy).should(never()).syncVariants();
    }

    @Test
    @DisplayName("옵션값 수정 실패 - 존재하지 않는 상품 ID면 ProductNotFoundException이 터진다")
    void updateOptionValueFailProductNotFound() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        OptionValueUpdateRequest request = new OptionValueUpdateRequest("레드", 500);

        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.updateOptionValue(productId, optionValueId, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("옵션값 수정 실패 - 상품은 존재하지만 해당 옵션값 ID를 찾을 수 없으면 ProductOptionValueNotFoundException이 터진다")
    void updateOptionValueFailOptionValueNotFound() {
        // given
        UUID productId = UUID.randomUUID();
        UUID wrongOptionValueId = UUID.randomUUID();
        UUID actualOptionValueId = UUID.randomUUID();
        OptionValueUpdateRequest request = new OptionValueUpdateRequest("블루", 1000);

        Product product = createProduct();
        ProductOption option = ProductOption.addOption(product, "컬러");

        ProductOptionValue actualValue = spy(ProductOptionValue.addOptionValue(option, "옐로우", 0));
        given(actualValue.getId()).willReturn(actualOptionValueId);

        option.getOptionValues().clear();
        option.getOptionValues().add(actualValue);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.updateOptionValue(productId, wrongOptionValueId, request))
                .isInstanceOf(ProductOptionValueNotFoundException.class);
    }

    @Test
    @DisplayName("상품 상태 변경 성공 - ACTIVE 상품을 INACTIVE로 변경 시 상품 및 하위 모든 옵션값이 판매 중지된다")
    void updateProductStatusToInactiveSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        ProductStatusRequest request = new ProductStatusRequest(ProductStatus.INACTIVE);

        Product product = createProduct();

        ProductOption option = ProductOption.addOption(product, "사이즈");
        ProductOptionValue optionValueSpy = spy(ProductOptionValue.addOptionValue(option, "XL", 0));

        option.getOptionValues().clear();
        option.getOptionValues().add(optionValueSpy);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        ProductStatusResponse response = productService.updateProductStatus(productId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);

        then(optionValueSpy).should(times(1)).stopSelling();
    }

    @Test
    @DisplayName("상품 상태 변경 성공 - INACTIVE 상품을 ACTIVE로 변경 시 상품 및 하위 모든 옵션값이 판매 재개된다")
    void updateProductStatusToActiveSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        ProductStatusRequest request = new ProductStatusRequest(ProductStatus.ACTIVE);

        Product product = createProduct();
        product.stopSelling();

        ProductOption option = ProductOption.addOption(product, "사이즈");
        ProductOptionValue optionValueSpy = spy(ProductOptionValue.addOptionValue(option, "L", 0));

        option.getOptionValues().clear();
        option.getOptionValues().add(optionValueSpy);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        ProductStatusResponse response = productService.updateProductStatus(productId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        then(optionValueSpy).should(times(1)).resumeSelling();
    }

    @Test
    @DisplayName("상품 상태 변경 실패 - 이미 INACTIVE 상태인 상품을 또 INACTIVE로 변경 요청하면 UnsupportedProductStatus 예외가 터진다")
    void updateProductStatusFailUnsupportedStatus() {
        // given
        UUID productId = UUID.randomUUID();
        ProductStatusRequest request = new ProductStatusRequest(ProductStatus.INACTIVE);

        Product product = createProduct();
        product.stopSelling();

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.updateProductStatus(productId, request))
                .isInstanceOf(UnsupportedProductStatus.class);
    }

    @Test
    @DisplayName("상품 상태 변경 실패 - 존재하지 않는 상품 ID면 ProductNotFoundException이 터진다")
    void updateProductStatusFailProductNotFound() {
        // given
        UUID productId = UUID.randomUUID();
        ProductStatusRequest request = new ProductStatusRequest(ProductStatus.ACTIVE);

        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.updateProductStatus(productId, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("옵션값 상태 변경 성공 - ACTIVE 옵션값을 INACTIVE로 변경 시 하위 배리언트도 판매 중지된다")
    void updateOptionStatusToInactiveSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        OptionValueStatusRequest request = new OptionValueStatusRequest(ProductStatus.INACTIVE);

        Product product = createProduct();
        ProductOption option = ProductOption.addOption(product, "용량");

        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "256GB", 0));
        given(targetValue.getId()).willReturn(optionValueId);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        ProductVariant variantSpy = spy(ProductVariant.addVariant(product, "SKU-V01", "256G 모델", 50000));
        product.getVariants().clear();
        product.getVariants().add(variantSpy);

        ProductVariantOption mappingMock = mock(ProductVariantOption.class);
        given(mappingMock.getVariants()).willReturn(variantSpy);

        targetValue.getVariantOptions().add(mappingMock);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        List<VariantResponse> responses = productService.updateOptionStatus(productId, optionValueId, request);

        // then
        assertThat(responses).hasSize(1);

        then(variantSpy).should(times(1)).stopSelling();
    }

    @Test
    @DisplayName("옵션값 상태 변경 성공 - 상품이 ACTIVE 상태일 때, INACTIVE 옵션값을 ACTIVE로 변경하면 하위 배리언트도 판매 재개된다")
    void updateOptionStatusToActiveSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        OptionValueStatusRequest request = new OptionValueStatusRequest(ProductStatus.ACTIVE);

        Product product = createProduct();

        ProductOption option = ProductOption.addOption(product, "용량");

        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "512GB", 0));
        given(targetValue.getId()).willReturn(optionValueId);


        given(targetValue.getStatus()).willReturn(ProductStatus.INACTIVE);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        ProductVariant variantSpy = spy(ProductVariant.addVariant(product, "SKU-V02", "512G 모델", 80000));
        product.getVariants().clear();
        product.getVariants().add(variantSpy);

        ProductVariantOption mappingMock = mock(ProductVariantOption.class);
        given(mappingMock.getVariants()).willReturn(variantSpy);
        targetValue.getVariantOptions().add(mappingMock);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        List<VariantResponse> responses = productService.updateOptionStatus(productId, optionValueId, request);

        // then
        assertThat(responses).hasSize(1);

        then(variantSpy).should(times(1)).resumeSelling();
    }

    @Test
    @DisplayName("옵션값 상태 변경 실패 - 상위 상품이 INACTIVE 상태이면 하위 옵션값을 ACTIVE로 변경할 수 없다")
    void updateOptionStatusToActiveFailWhenProductInactive() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        OptionValueStatusRequest request = new OptionValueStatusRequest(ProductStatus.ACTIVE);

        Product product = createProduct();
        product.stopSelling();

        ProductOption option = ProductOption.addOption(product, "용량");

        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "512GB", 0));
        given(targetValue.getId()).willReturn(optionValueId);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.updateOptionStatus(productId, optionValueId, request))
                .isInstanceOf(UnsupportedProductStatus.class);
    }

    @Test
    @DisplayName("옵션값 상태 변경 실패 - 상위 상품이 이미 INACTIVE 상태이면 하위 옵션값을 INACTIVE로 변경할 수 없다")
    void updateOptionStatusToInactiveFailWhenProductInactive() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        OptionValueStatusRequest request = new OptionValueStatusRequest(ProductStatus.INACTIVE);

        Product product = createProduct();
        product.stopSelling();

        ProductOption option = ProductOption.addOption(product, "용량");

        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "512GB", 0));
        given(targetValue.getId()).willReturn(optionValueId);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when & then
        // 🚀 상품이 이미 INACTIVE이므로 첫 번째 if 블록의 상품 상태 체크에서 튕겨야 합니다!
        assertThatThrownBy(() -> productService.updateOptionStatus(productId, optionValueId, request))
                .isInstanceOf(UnsupportedProductStatus.class);
    }

    @Test
    @DisplayName("옵션값 상태 변경 실패 - 옵션값이 이미 ACTIVE 상태인데 또 ACTIVE로 변경 요청하면 예외가 터진다")
    void updateOptionStatusFailDuplicateStatus() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        OptionValueStatusRequest request = new OptionValueStatusRequest(ProductStatus.ACTIVE);

        Product product = createProduct();
        ProductOption option = ProductOption.addOption(product, "용량");

        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "512GB", 0));
        given(targetValue.getId()).willReturn(optionValueId);
        given(targetValue.getStatus()).willReturn(ProductStatus.ACTIVE);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.updateOptionStatus(productId, optionValueId, request))
                .isInstanceOf(UnsupportedProductStatus.class);
    }

    @Test
    @DisplayName("상품 삭제 성공 - 존재하는 상품 ID면 해당 상품을 softDelete(삭제 처리)한다")
    void deleteProductSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        String companyMemberId = UUID.randomUUID().toString();

        Product productSpy = spy(createProduct());

        given(productRepository.findById(productId)).willReturn(Optional.of(productSpy));

        // when
        productService.deleteProduct(productId, companyMemberId);

        // then
        then(productSpy).should(times(1)).delete(companyMemberId);


         assertThat(productSpy.getStatus()).isEqualTo(ProductStatus.DELETED);
    }

    @Test
    @DisplayName("상품 삭제 실패 - 존재하지 않는 상품 ID면 ProductNotFoundException이 터진다")
    void deleteProductFailProductNotFound() {
        // given
        UUID productId = UUID.randomUUID();
        String companyMemberId = UUID.randomUUID().toString();

        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.deleteProduct(productId, companyMemberId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("옵션 삭제 성공 - 존재하는 상품과 옵션값이면 옵션값을 삭제하고 하위 배리언트들도 함께 연쇄 삭제한다")
    void deleteOptionSuccess() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        String companyMemberId = UUID.randomUUID().toString();

        Product product = createProduct();
        ProductOption option = ProductOption.addOption(product, "사이즈");

        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "FREE", 0));
        given(targetValue.getId()).willReturn(optionValueId);
        given(targetValue.getStatus()).willReturn(ProductStatus.ACTIVE);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        ProductVariant variantSpy = spy(ProductVariant.addVariant(product, "SKU-D01", "프리 사이즈 모델", 20000));
        product.getVariants().clear();
        product.getVariants().add(variantSpy);

        ProductVariantOption mappingMock = mock(ProductVariantOption.class);
        given(mappingMock.getVariants()).willReturn(variantSpy);
        targetValue.getVariantOptions().add(mappingMock);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        productService.deleteOption(productId, optionValueId, companyMemberId);

        // then
        then(targetValue).should(times(1)).delete(companyMemberId);
        then(variantSpy).should(times(1)).delete(companyMemberId);
    }

    @Test
    @DisplayName("옵션 삭제 실패 - 상위 상품이 이미 DELETED 상태이면 UnsupportedProductStatus 예외가 터진다")
    void deleteOptionFailWhenProductDeleted() {
        // given
        UUID productId = UUID.randomUUID();
        UUID optionValueId = UUID.randomUUID();
        String companyMemberId = UUID.randomUUID().toString();

        Product product = createProduct();
        product.delete(companyMemberId);

        ProductOption option = ProductOption.addOption(product, "사이즈");
        ProductOptionValue targetValue = spy(ProductOptionValue.addOptionValue(option, "FREE", 0));
        given(targetValue.getId()).willReturn(optionValueId);

        option.getOptionValues().clear();
        option.getOptionValues().add(targetValue);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.deleteOption(productId, optionValueId, companyMemberId))
                .isInstanceOf(UnsupportedProductStatus.class);
    }

    @Test
    @DisplayName("배리언트 검증 성공 - 요청한 모든 배리언트가 ACTIVE 상태면 누락 없이 모두 반환된다")
    void validateVariantByIdAllActiveSuccess() {
        // given
        UUID variantId1 = UUID.randomUUID();
        UUID variantId2 = UUID.randomUUID();
        List<UUID> productVariantIds = List.of(variantId1, variantId2);

        Product product = createProduct();
        ProductVariant variant1 = spy(ProductVariant.addVariant(product, "SKU-001", "블랙 XL", 10000));
        ProductVariant variant2 = spy(ProductVariant.addVariant(product, "SKU-002", "블랙 L", 10000));

        given(variant1.getId()).willReturn(variantId1);
        given(variant2.getId()).willReturn(variantId2);
        given(variant1.getStatus()).willReturn(ProductStatus.ACTIVE);
        given(variant2.getStatus()).willReturn(ProductStatus.ACTIVE);

        given(productVariantRepository.findAllById(productVariantIds)).willReturn(List.of(variant1, variant2));

        // when
        List<VariantResponse> responses = productService.validateVariantById(productVariantIds);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).variantId()).isEqualTo(variantId1);
        assertThat(responses.get(1).variantId()).isEqualTo(variantId2);
    }

    @Test
    @DisplayName("배리언트 검증 성공 - 요청한 배리언트 중 INACTIVE 상태인 것은 필터링되어 ACTIVE한 것만 반환된다")
    void validateVariantByIdFilterInactive() {
        // given
        UUID activeVariantId = UUID.randomUUID();
        UUID inactiveVariantId = UUID.randomUUID();
        List<UUID> productVariantIds = List.of(activeVariantId, inactiveVariantId);

        Product product = createProduct();

        ProductVariant activeVariant = spy(ProductVariant.addVariant(product, "SKU-ACTIVE", "화이트 XL", 12000));
        ProductVariant inactiveVariant = spy(ProductVariant.addVariant(product, "SKU-INACTIVE", "화이트 L (품절)", 12000));

        given(activeVariant.getId()).willReturn(activeVariantId);

        given(activeVariant.getStatus()).willReturn(ProductStatus.ACTIVE);
        given(inactiveVariant.getStatus()).willReturn(ProductStatus.INACTIVE);

        given(productVariantRepository.findAllById(productVariantIds)).willReturn(List.of(activeVariant, inactiveVariant));

        // when
        List<VariantResponse> responses = productService.validateVariantById(productVariantIds);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).variantId()).isEqualTo(activeVariantId);
    }

    @Test
    @DisplayName("배리언트 검증 성공 - 요청 ID 리스트가 null이거나 비어있으면 DB 조회 없이 빈 리스트를 반환한다")
    void validateVariantByIdEmptyOrNull() {
        // when & then 1: null 보냈을 때
        List<VariantResponse> nullResult = productService.validateVariantById(null);
        assertThat(nullResult).isEmpty();

        // when & then 2: 빈 리스트 보냈을 때
        List<VariantResponse> emptyResult = productService.validateVariantById(Collections.emptyList());
        assertThat(emptyResult).isEmpty();

        then(productVariantRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("허브 조회 성공 - 존재하는 배리언트 ID 리스트를 넘기면 연관된 허브 정보 리스트가 정상 반환된다")
    void getVariantHubSuccess() {
        // given
        UUID variantId = UUID.randomUUID();
        List<UUID> productVariantIds = List.of(variantId);

        Product product = createProduct();
        ProductVariant variantSpy = spy(ProductVariant.addVariant(product, "SKU-H01", "허브 추적 상품", 50000));
        given(variantSpy.getId()).willReturn(variantId);

        given(productVariantRepository.findAllWithProductByIdIn(productVariantIds))
                .willReturn(List.of(variantSpy));

        // when
        List<VariantHubResponse> responses = productService.getVariantHub(productVariantIds);

        // then
        assertThat(responses).hasSize(1);

        assertThat(responses.get(0).variantId()).isEqualTo(variantId);
    }

    @Test
    @DisplayName("허브 조회 성공 - 요청 ID 리스트가 null이거나 비어있으면 DB 조회 없이 즉시 빈 리스트를 반환한다")
    void getVariantHubEmptyOrNull() {
        // when & then 1: null 보냈을 때
        List<VariantHubResponse> nullResult = productService.getVariantHub(null);
        assertThat(nullResult).isEmpty();

        // when & then 2: 빈 리스트 보냈을 때
        List<VariantHubResponse> emptyResult = productService.getVariantHub(Collections.emptyList());
        assertThat(emptyResult).isEmpty();

        then(productVariantRepository).shouldHaveNoInteractions();
    }
}