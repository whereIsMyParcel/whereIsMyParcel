package com.sparta.whereismyparcel.company.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.company.application.service.CompanyService;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyMemberRequest;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyRegisterRequest;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyUpdateRequest;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyListResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyMemberResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "업체 등록", description = "가입 시 사업자번호를 가진 유저가 최초 매니저로 지정됩니다.")
    @PostMapping
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> registerCompany(
            @RequestBody @Valid CompanyRegisterRequest request
            ) {
        CompanyResponse response = companyService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "업체 조회", description = "해당 업체의 정보를 조회합니다")
    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(@PathVariable UUID companyId) {
        return ResponseEntity.ok(ApiResponse.success(companyService.getCompany(companyId)));
    }

    @Operation(summary = "업체 목록 조회", description = "업체 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CompanyListResponse>>> getCompanies (
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CompanyListResponse> companies = companyService.getCompanies(pageable);
        return ResponseEntity.ok(ApiResponse.success(companies));
    }

    @Operation(summary = "업체 수정", description = "업체 수정")
    @PatchMapping("/{companyId}")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable UUID companyId,
            @RequestBody @Valid CompanyUpdateRequest request
    ) {
        CompanyResponse response = companyService.updateCompanyDetails(companyId,request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "업체 삭제", description = "업체 삭제")
    @DeleteMapping("/{companyId}")
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(
            @PathVariable UUID companyId,
            @RequestHeader("X-User-Id") String hubManagerOrMasterId
    ) {
        companyService.deleteCompany(companyId, hubManagerOrMasterId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "업체 멤버 등록", description = "업체 멤버 등록")
    @PostMapping("/{companyId}/member")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<CompanyMemberResponse>> registerCompanyMember(
            @PathVariable UUID companyId,
            @RequestBody @Valid CompanyMemberRequest request
    ) {
        CompanyMemberResponse response = companyService.registerCompanyMember(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "업체 멤버 조회", description = "해당 업체 멤버의 정보를 조회합니다")
    @GetMapping("/{companyId}/member/{memberId}")
    public ResponseEntity<ApiResponse<CompanyMemberResponse>> getCompanyMember(
            @PathVariable UUID companyId,
            @PathVariable UUID memberId
    )  {
        return ResponseEntity.ok(ApiResponse.success(companyService.getCompanyMember(companyId, memberId)));
    }

    @Operation(summary = "업체 멤버 삭제", description = "업체 멤버 삭제")
    @DeleteMapping("/{companyId}/member")
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCompanyMember(
            @PathVariable UUID companyId,
            @RequestBody @Valid CompanyMemberRequest request,
            @RequestHeader("X-User-Id") String hubManagerOrMasterId
    ) {
        companyService.deleteCompanyMember(companyId, request, hubManagerOrMasterId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
