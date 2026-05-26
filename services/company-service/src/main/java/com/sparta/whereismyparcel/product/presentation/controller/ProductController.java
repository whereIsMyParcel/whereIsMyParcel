package com.sparta.whereismyparcel.product.presentation.controller;

import com.sparta.whereismyparcel.common.dto.PageResponse;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.product.application.service.ProductService;
import com.sparta.whereismyparcel.product.presentation.dto.request.*;
import com.sparta.whereismyparcel.product.presentation.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Operation(summary = "상품 등록", description = "COMPANY_MANAGER")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> registerProduct(
            @RequestBody @Valid ProductRegisterRequest registerRequest) {
        ProductResponse response = productService.registerProduct(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "상품 조회", description = "ALL")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProduct(productId)));
    }

    @Operation(summary = "상품 페이징 조회", description = "ALL")
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductPageResponse>>> getProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable
    ) {
        Page<ProductPageResponse> responses = productService.getProducts(pageable);
        PageResponse<ProductPageResponse> pageResponse = PageResponse.from(responses);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @Operation(summary = "상품 옵션조합 조회", description = "ALL")
    @GetMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> getVariants(@PathVariable UUID productId) {
        List<VariantResponse> response = productService.getVariants(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 수정", description = "COMPANY_MANAGER")
    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<ProductUpdateResponse>> updateProduct(
            @PathVariable UUID productId,
            @RequestBody @Valid ProductUpdateRequest request) {
        ProductUpdateResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 옵션 수정", description = "COMPANY_MANAGER")
    @PatchMapping("/{productId}/optionValues/{optionValueId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> updateOption(
            @PathVariable UUID productId,
            @PathVariable UUID optionValueId,
            @RequestBody @Valid OptionValueUpdateRequest request) {
        List<VariantResponse> response = productService.updateOptionValue(productId, optionValueId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 상태 변경", description = "COMPANY_MANAGER")
    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<ProductStatusResponse>> updateProductStatus(
            @PathVariable UUID productId,
            @RequestBody @Valid ProductStatusRequest request) {
        ProductStatusResponse response = productService.updateProductStatus(productId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 옵션 상태 변경", description = "COMPANY_MANAGER")
    @PatchMapping("/{productId}/optionValues/{optionValueId}/status")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> updateOptionStatus(
            @PathVariable UUID productId,
            @PathVariable UUID optionValueId,
            @RequestBody @Valid OptionValueStatusRequest request) {
        List<VariantResponse> response = productService.updateOptionStatus(productId, optionValueId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 삭제", description = "COMPANY_MANAGER")
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") String companyMemberId) {
        productService.deleteProduct(productId, companyMemberId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "상품 옵션 삭제", description = "COMPANY_MANAGER")
    @DeleteMapping("/{productId}/optionValues/{optionValueId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteOption(
            @PathVariable UUID productId,
            @PathVariable UUID optionValueId,
            @RequestHeader("X-User-Id") String companyMemberId) {
        productService.deleteOption(productId, optionValueId, companyMemberId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
