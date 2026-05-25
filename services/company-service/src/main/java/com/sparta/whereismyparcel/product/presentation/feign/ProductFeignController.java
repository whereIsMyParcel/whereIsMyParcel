package com.sparta.whereismyparcel.product.presentation.feign;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.product.application.service.ProductService;
import com.sparta.whereismyparcel.product.presentation.dto.response.VariantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/products")
public class ProductFeignController {

    private final ProductService productService;

    /**
     * 주문서 생성 전 베리언트 ID 목록 유효성 검증 및 단가 조회
     */
    @GetMapping
    public ApiResponse<List<VariantResponse>> validateProduct(
            @RequestParam List<UUID> productVariantIds){
        List<VariantResponse> response = productService.validateVariantById(productVariantIds);
        return ApiResponse.success(response);
    }
}
