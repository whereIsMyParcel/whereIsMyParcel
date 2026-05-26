package com.sparta.whereismyparcel.product.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.product.application.service.ProductService;
import com.sparta.whereismyparcel.product.presentation.dto.request.*;
import com.sparta.whereismyparcel.product.presentation.dto.response.ProductResponse;
import com.sparta.whereismyparcel.product.presentation.dto.response.ProductStatusResponse;
import com.sparta.whereismyparcel.product.presentation.dto.response.ProductUpdateResponse;
import com.sparta.whereismyparcel.product.presentation.dto.response.VariantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    // 상품 등록
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> registerProduct(
            @RequestBody @Valid ProductRegisterRequest registerRequest) {
        ProductResponse response = productService.registerProduct(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    // 상품조회
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProduct(productId)));
    }

    // 옵션 조합 조회
    @GetMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> getVariants(@PathVariable UUID productId) {
        List<VariantResponse> response = productService.getVariants(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 상품 수정
    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<ProductUpdateResponse>> updateProduct(
            @PathVariable UUID productId,
            @RequestBody @Valid ProductUpdateRequest request) {
        ProductUpdateResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 상품 옵션 수정
    @PatchMapping("/{productId}/optionValues/{optionValueId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> updateOption(
            @PathVariable UUID productId,
            @PathVariable UUID optionValueId,
            @RequestBody @Valid OptionValueUpdateRequest request) {
        List<VariantResponse> response = productService.updateOptionValue(productId, optionValueId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 상품 상태 변경
    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<ProductStatusResponse>> updateProductStatus(
            @PathVariable UUID productId,
            @RequestBody @Valid ProductStatusRequest request) {
        ProductStatusResponse response = productService.updateProductStatus(productId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 상품 옵션 상태 변경
    @PatchMapping("/{productId}/optionValues/{optionValueId}/status")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> updateOptionStatus(
            @PathVariable UUID productId,
            @PathVariable UUID optionValueId,
            @RequestBody @Valid OptionValueStatusRequest request) {
        List<VariantResponse> response = productService.updateOptionStatus(productId, optionValueId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 상품 삭제
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") String companyMemberId) {
        productService.deleteProduct(productId, companyMemberId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 상품 옵션 삭제
    @DeleteMapping("/{productId}/optionValues/{optionValueId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteOption(
            @PathVariable UUID productId,
            @PathVariable UUID optionValueId,
            @RequestHeader("X-User-Id") String companyMemberId,
            @RequestBody @Valid OptionValueStatusRequest request) {
        productService.deleteOption(productId, optionValueId, companyMemberId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
