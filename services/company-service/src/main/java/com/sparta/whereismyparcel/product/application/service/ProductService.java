package com.sparta.whereismyparcel.product.application.service;

import com.sparta.whereismyparcel.product.domain.entity.*;
import com.sparta.whereismyparcel.product.domain.exception.ProductNotFoundException;
import com.sparta.whereismyparcel.product.domain.exception.ProductOptionValueNotFoundException;
import com.sparta.whereismyparcel.product.domain.exception.UnsupportedProductStatus;
import com.sparta.whereismyparcel.product.domain.repository.ProductOptionRepository;
import com.sparta.whereismyparcel.product.domain.repository.ProductOptionValueRepository;
import com.sparta.whereismyparcel.product.domain.repository.ProductRepository;
import com.sparta.whereismyparcel.product.domain.repository.ProductVariantRepository;
import com.sparta.whereismyparcel.product.presentation.dto.request.*;
import com.sparta.whereismyparcel.product.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionValueRepository productOptionValueRepository;


    // 상품 등록
    @Transactional
    public ProductResponse registerProduct(ProductRegisterRequest request) {
        // 1. 최상위 상품 객체를 생성 [대대장]
        Product product = Product.create(
                request.name(),
                request.companyId(),
                request.hubId(),
                request.description(),
                request.price()
        );

        // 2. 최상위 상품을 명시적 영속화
        // 상품 기본정보를 영속성 1차 캐시에 저장[대대장] -> 이때 UUID가 발급되어 저장 쓰기지연으로 저장
        Product savedProduct = productRepository.save(product);

        // 옵션이 없는 경우
        if (request.options() == null || request.options().isEmpty()) {
            String skuCode = "SKU-" + savedProduct.getId().toString().substring(0, 8).toUpperCase() + "-001";

            ProductVariant defaultVariant = ProductVariant.addVariant(
                    savedProduct,
                    skuCode,
                    savedProduct.getName(),
                    savedProduct.getPrice()
            );
            ProductVariant savedVariant = productVariantRepository.save(defaultVariant);

            List<OptionResponse> emptyOptions = Collections.emptyList();
            List<VariantResponse> variants = List.of(VariantResponse.from(savedVariant));
            return ProductResponse.from(savedProduct, emptyOptions, variants);
        }

        // 3. 최종 API응답을 계층형 구조로 빌드하기 위해 빈 리스트 생성
        List<OptionResponse> optionResponses = new ArrayList<>(); // 최종 옵션
        List<VariantResponse> variantResponses = new ArrayList<>(); // 최종 조합

        // 4. CartesianProduct(데카르트 곱) 알고리즘에게 줄 '옵션 그룹 주머니'생성 [2차원 행렬 구조]
        // ex) [[화이트, 블랙], [260,270]]
        List<List<ProductOptionValue>> optionValue = new ArrayList<>();

        // 5. 요청 데이터에 업체에서 입력한 옵션을 리스트 순회하며 저장
        request.options().forEach(optReq -> {

            // 5-1 옵션 엔티티 객체 생성
            ProductOption option = ProductOption.addOption(savedProduct, optReq.name());
            // 5-2 생성된 객체를 명시적 db에 저장 -> 영속화
            ProductOption savedOption = productOptionRepository.save(option);

            // 5-3 이번 루트에 속한 하위 옵션값 엔티티들을 일시적으로 묶을 리스트 생성
            List<ProductOptionValue> savedOptionValue = new ArrayList<>();
            // 5-4 해당 옵션 하위에 딸려나갈 응답용 하위 값 리스트 생성
            List<OptionValueResponse> valueResponses = new ArrayList<>();

            // 5-5 해당 옵션 하위에 정의된 실제 옵션ㄱ밧 리스트를 2중 루프로 순회
            optReq.optionValue().forEach(optValReq -> {
                // 5-5-1 옵션 벨류 엔티티 객체 생성
                ProductOptionValue value = ProductOptionValue.addOptionValue(savedOption, optValReq.value(), optValReq.additionalPrice());
                // 5-5-2 명시적으로 db 저장 -> 영속화
                ProductOptionValue savedValue = productOptionValueRepository.save(value);

                // 5-5-3 생성된 객체를 리스트에 적제
                savedOptionValue.add(value);
                // 5-5-4 응답해줄 DTO로 변환해 저장
                valueResponses.add(OptionValueResponse.from(savedValue));
            });

            // 5-6  2중 루프가 끝난 한 카테고리([260,270])의 묶음을 통째로 2차원 배열에 저장
            optionValue.add(savedOptionValue);
            // 5-7 최상위 응답 DTO에 저장하기 위해 옵션 이름과 옵션값 DTO리스트를 조립하여 저장
            optionResponses.add(OptionResponse.from(savedOption, valueResponses));
        });

        // 6. 저장한 2차원 배열([[화이트, 블랙], [260, 270]])을 재귀함수 서비스에 보내 데카르트 곱 연산
        // 데카르트 곱을 자동 생성 [[화이트,260]], [[화이트,270]] ....
       List<List<ProductOptionValue>> combinations = CartesianProduct.of(optionValue);

       // 7. 자동 생성된 조합목록을 바탕으로 재고에 보관할 조합으로 변경
       int variantSequence = 1; // SKU 뒷자리 일련번호를 매겨주기위한 시퀀스 카운트
       for (List<ProductOptionValue> combination :  combinations) {
           int totalAdditionalPrice = combination.stream()
                   .mapToInt(ProductOptionValue::getAdditionalPrice)
                   .sum();

           int finalVariantPrice = savedProduct.getPrice() +  totalAdditionalPrice;

           // 7-1 기본 상품명을 기준으로 상품명 생성 (나이키 에어포스 '07)
           StringBuilder variantName = new StringBuilder(savedProduct.getName()).append(" (");

           // 7-2 옵션 값들을 순회하며 이름을 이어 붙임
           for (int i = 0; i < combination.size(); i++) {
               variantName.append(combination.get(i).getValue()); // 갑 바인딩 (화이트)

               // 옵션값이 더 있으면 / 통해 구분
               if (i < combination.size() - 1) {
                   variantName.append(" / ");
               }
           }
           // 최종 형태 "나이키 에어포스 '07 (화이트 / 260)
           variantName.append(")");

           // 7-3 SKU코드 자동 부여
           // [SKU - 상품 UUID 앞8자리 대문자 - 3자리 순차 번호 ( SKU-A748BSC-001)
           String skuCode = "SKU-" + savedProduct.getId().toString().substring(0, 8).toUpperCase()
                   + "-" + String.format("%03d", variantSequence++);

           // 7-4 옵션 조합 객체 생성
           ProductVariant variant = ProductVariant.addVariant(
                   savedProduct,
                   skuCode,
                   variantName.toString(),
                   finalVariantPrice
           );

           // 7-5 실제 옵션의 조합을 db에 저장 -> 영속화
           ProductVariant savedVariant = productVariantRepository.save(variant);

           // 7-6 응답용 DTO에 저장
           variantResponses.add(VariantResponse.from(savedVariant));

           // 7-7 다대다 연관관계 매핑
           // 소대장을 하나씩 꺼내 행정병을 통해
           // 각 중대장 db에 저장
           for (ProductOptionValue value : combination) {
               ProductVariantOption.addVariantOption(savedVariant,value);
           }
       }

       // 8. 종합적으로 만들어진 상품을 응답
       return ProductResponse.from(savedProduct,optionResponses,variantResponses);
    }

    // 상품 조회
    // TODO : 삭제된 상품도 전부 보여줘야 한다고 생각이 드는데 그러면 @SQLRestriction을 지우면 됩니다
    // TODO : 어떻게 하면 좋을까요?
    public ProductResponse getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        List<OptionResponse> optionResponses =
                product.getOptions().stream()
                        .map(option -> {

                            List<OptionValueResponse> valueResponses =
                                    option.getOptionValues().stream()
                                            .map(OptionValueResponse::from)
                                            .toList();
                            return OptionResponse.from(option, valueResponses);
                        })
                        .toList();
        List<VariantResponse> variantResponses =
                product.getVariants().stream()
                        .map(VariantResponse::from)
                        .toList();
        return ProductResponse.from(product,optionResponses,variantResponses);
    }

    // 상품 목록 조회
    public Page<ProductPageResponse> getProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAllByDeletedAtIsNull(pageable);

        return products.map(ProductPageResponse::from);
    }

    // 상품 아이디를 기반으로 모든 베리언트 조회
    public List<VariantResponse> getVariants(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        return  product.getVariants().stream()
                .map(VariantResponse::from)
                .toList();
    }

    // 상품 수정
    @Transactional
    public ProductUpdateResponse updateProduct(UUID productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        product.updateDetails(request.name(), request.description(), request.price());
        product.getVariants().forEach(ProductVariant::syncVariants);

        return ProductUpdateResponse.from(product);
    }

    // 상품 옵션값 수정
    @Transactional
    public List<VariantResponse> updateOptionValue(UUID productId, UUID optionValueId, OptionValueUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        ProductOptionValue targetValue = product.getOptions().stream()
                .flatMap(option -> option.getOptionValues().stream())
                .filter(value -> value.getId().equals(optionValueId))
                .findFirst()
                .orElseThrow(ProductOptionValueNotFoundException::new);

        targetValue.updateValueDetails(request.value(), request.additionalPrice());

        product.getVariants().stream()
                .filter(variant -> variant.containsOptionValue(targetValue))
                .forEach(ProductVariant::syncVariants);

        return product.getVariants().stream()
                .map(VariantResponse::from)
                .toList();
    }

    // 상품 상태 변경
    @Transactional
    public ProductStatusResponse updateProductStatus(UUID productId, ProductStatusRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (request.status() == ProductStatus.INACTIVE) {
            if (product.getStatus() == ProductStatus.INACTIVE || product.getStatus() == ProductStatus.DELETED) {
                throw new UnsupportedProductStatus();
            }
            product.stopSelling();

            product.getOptions().stream()
                    .flatMap(option -> option.getOptionValues().stream())
                    .forEach(ProductOptionValue::stopSelling);

        } else if (request.status() == ProductStatus.ACTIVE) {
            if (product.getStatus() == ProductStatus.ACTIVE || product.getStatus() == ProductStatus.DELETED) {
                throw new UnsupportedProductStatus();
            }
            product.resumeSelling();

            product.getOptions().stream()
                    .flatMap(option -> option.getOptionValues().stream())
                    .forEach(ProductOptionValue::resumeSelling);
        }

        return ProductStatusResponse.from(product);
    }

    // 옵션 값 상태 변경
    @Transactional
    public List<VariantResponse> updateOptionStatus(UUID productId, UUID optionValueId ,OptionValueStatusRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        ProductOptionValue targetValue = product.getOptions().stream()
                .flatMap(option -> option.getOptionValues().stream())
                .filter(value -> value.getId().equals(optionValueId))
                .findFirst()
                .orElseThrow(ProductOptionValueNotFoundException::new);

        if (request.status() == ProductStatus.INACTIVE) {
            if (product.getStatus() == ProductStatus.INACTIVE || product.getStatus() == ProductStatus.DELETED) {
                throw new UnsupportedProductStatus();
            }
            if (targetValue.getStatus() == ProductStatus.INACTIVE) {
                throw new UnsupportedProductStatus();
            }
            targetValue.stopSelling();

            targetValue.getVariantOptions().stream()
                .map(ProductVariantOption::getVariants)
                .forEach(ProductVariant::stopSelling);

        } else if (request.status() == ProductStatus.ACTIVE) {
            if (product.getStatus() == ProductStatus.ACTIVE || product.getStatus() == ProductStatus.DELETED) {
                throw new UnsupportedProductStatus();
            }
            if (targetValue.getStatus() == ProductStatus.ACTIVE) {
                throw new UnsupportedProductStatus();
            }
            targetValue.resumeSelling();

            targetValue.getVariantOptions().stream()
                    .map(ProductVariantOption::getVariants)
                    .forEach(ProductVariant::resumeSelling);
        }

        return product.getVariants().stream()
                .map(VariantResponse::from)
                .toList();
    }

    // 상품 삭제(softDelete)
    @Transactional
    public void deleteProduct(UUID productId, String companyMemberId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        product.delete(companyMemberId);
    }

    // 상품 옵션 삭제(softDelete)
    @Transactional
    public void deleteOption(UUID productId, UUID optionValueId, String companyMemberId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        ProductOptionValue targetValue = product.getOptions().stream()
                .flatMap(option -> option.getOptionValues().stream())
                .filter(value -> value.getId().equals(optionValueId))
                .findFirst()
                .orElseThrow(ProductOptionValueNotFoundException::new);


        if (product.getStatus() == ProductStatus.DELETED) {
            throw new UnsupportedProductStatus();
        } else if (targetValue.getStatus() == ProductStatus.DELETED) {
            throw new UnsupportedProductStatus();
        }

        targetValue.delete(companyMemberId);

        targetValue.getVariantOptions().stream()
                .map(ProductVariantOption::getVariants)
                .forEach(variants -> variants.delete(companyMemberId));
    }

    /**
     * ③ 주문 생성 전 상품 상태 확인 (Order ➡️ Product)
     */
    public List<VariantResponse> validateVariantById(List<UUID> productVariantIds){
        if (productVariantIds == null || productVariantIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return productVariantRepository.findAllById(productVariantIds).stream()
                .filter(variant -> variant.getStatus() == ProductStatus.ACTIVE)
                .map(VariantResponse::from)
                .toList();
    }
}
