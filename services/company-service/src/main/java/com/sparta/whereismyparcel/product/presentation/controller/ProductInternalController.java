package com.sparta.whereismyparcel.product.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.product.application.service.ProductService;
import com.sparta.whereismyparcel.product.presentation.dto.response.VariantHubResponse;
import com.sparta.whereismyparcel.product.presentation.dto.response.VariantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/products")
public class ProductInternalController {

    private final ProductService productService;

    /**
     * 주문서 생성 전 베리언트 ID 목록 유효성 검증 및 단가 조회
     */
    @GetMapping("/valid")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> validateProduct(
            @RequestParam List<UUID> productVariantIds){
        List<VariantResponse> response = productService.validateVariantById(productVariantIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 배송 생성시 주문 상품들의 허브 조회 (Shipment ➡︎ Company) 리스트 조회
     */
    @GetMapping("/search-hub")
    public ResponseEntity<ApiResponse<List<VariantHubResponse>>> getVariantHub(
            @RequestParam("productVariantId") List<UUID> productVariantIds) {
        List<VariantHubResponse> response = productService.getVariantHub(productVariantIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
