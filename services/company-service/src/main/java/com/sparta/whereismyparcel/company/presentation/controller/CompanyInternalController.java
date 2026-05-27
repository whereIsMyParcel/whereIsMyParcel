package com.sparta.whereismyparcel.company.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.company.application.service.CompanyService;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanySearchHubRequest;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanySearchHubResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/companies")
public class CompanyInternalController {

    private final CompanyService companyService;

    /**
     * 배송 생성시 입력받은 주소로 목적지 허브 조회 (Shipment ➡︎ Company) 단일요청
     */
    @PostMapping("/search-hub")
    public ResponseEntity<ApiResponse<CompanySearchHubResponse>> searchHub(@RequestBody @Valid CompanySearchHubRequest request) {
        CompanySearchHubResponse response = companyService.getHub(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
