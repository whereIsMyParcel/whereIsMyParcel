package com.sparta.whereismyparcel.company.application.service;

import com.sparta.whereismyparcel.company.infrastructure.feign.client.HubFeignClient;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.company.domain.entity.Company;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMember;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMemberStatus;
import com.sparta.whereismyparcel.company.domain.entity.CompanyStatus;
import com.sparta.whereismyparcel.company.domain.exception.*;
import com.sparta.whereismyparcel.company.domain.repository.CompanyMemberRepository;
import com.sparta.whereismyparcel.company.domain.repository.CompanyRepository;
import com.sparta.whereismyparcel.company.infrastructure.feign.client.UserFeignClient;
import com.sparta.whereismyparcel.company.presentation.dto.request.*;
import com.sparta.whereismyparcel.company.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserFeignClient userFeignClient;
    private final HubFeignClient hubFeignClient;

    // 업체 등록
    @Transactional
    public CompanyResponse registerCompany(CompanyRegisterRequest request) {
        if (companyRepository.existsByBusinessNumber(request.businessNumber())) {
            throw new BusinessNumberIsExistsException();
        }
        if (companyRepository.existsByCompanyName(request.companyName())) {
            throw new CompanyNameIsExistsException();
        }

        ApiResponse<Boolean> hubCheck = hubFeignClient.isHubExists(request.hubId());
        if (hubCheck == null || !hubCheck.success() || Boolean.FALSE.equals(hubCheck.data())) {
            throw new HubNotFoundException();
        }

        ApiResponse<UserIdResponse> managerIdResponse = userFeignClient.getUserIdByBusinessNumber(request.businessNumber());
        if (managerIdResponse == null || !managerIdResponse.success() || managerIdResponse.data() == null) {
            throw new UserNotFoundException();
        }

        UUID managerId = managerIdResponse.data().userId();

        Company company = Company.create(
                request.hubId(),
                request.companyType(),
                request.companyName(),
                request.businessNumber(),
                request.managerName(),
                request.managerPhone(),
                request.zipCode(),
                request.address(),
                request.addressDetail()
        );

        Company savedCompany = companyRepository.save(company);

        ApiResponse<Void> updateUserResponse = userFeignClient.updateUserCompanyId(managerId, savedCompany.getId());
        if (updateUserResponse == null || !updateUserResponse.success()) {
            throw new UserSyncFailedException();
        }
        CompanyMember initialMember = CompanyMember.addMember(managerId, savedCompany);
        companyMemberRepository.save(initialMember);

        return CompanyResponse.from(savedCompany);
    }

    // 업체 조회
    public CompanyResponse getCompany(UUID companyId) {
        Company company = companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);
        return CompanyResponse.from(company);
    }

    // 업체 목록 조회
    public Page<CompanyListResponse> getCompanies(Pageable pageable) {
        Page<Company> companies = companyRepository.findAllCompaniesByStatus(CompanyStatus.ACTIVE, pageable);

        return companies.map(CompanyListResponse::from);
    }

    // 업체 수정
    @Transactional
    public CompanyResponse updateCompanyDetails(UUID companyId, CompanyUpdateRequest request) {
        Company company = companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);

        company.updateDetails(
                request.companyType(),
                request.companyName(),
                request.managerName(),
                request.managerPhone(),
                request.zipCode(),
                request.address(),
                request.addressDetail(),
                request.status());

        return CompanyResponse.from(companyRepository.save(company));
    }

    // 업체 삭제
    @Transactional
    public void deleteCompany(UUID companyId, String hubManagerOrMasterId) {
        Company company = companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);

        company.delete(hubManagerOrMasterId);

        ApiResponse<Void> userDeleteResponse = userFeignClient.deleteAllUsersInCompany(companyId);
        if (userDeleteResponse == null || !userDeleteResponse.success()) {
            throw new UserSyncFailedException();
        }
    }

    // 업체 직원 등록
    @Transactional
    public CompanyMemberResponse registerCompanyMember(UUID companyId, CompanyMemberRequest request) {
        Company company = companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);

        Boolean isExistsMember = companyMemberRepository.existsByUserId(request.memberUserId());
        if (isExistsMember) {
            throw new AlreadyRegisterMemberException();
        }

        ApiResponse<Void> updateUserResponse = userFeignClient.updateUserCompanyId(request.memberUserId(), companyId);
        if (updateUserResponse == null || !updateUserResponse.success()) {
            throw new UserSyncFailedException();
        }

        CompanyMember companyMember = CompanyMember.addMember(request.memberUserId(), company);
        companyMemberRepository.save(companyMember);
        return CompanyMemberResponse.from(companyMember);
    }

    // 직원 조회
    public CompanyMemberResponse getCompanyMember(UUID companyId, UUID memberId) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);
        CompanyMember companyMember = companyMemberRepository.findByIdAndStatus(memberId, CompanyMemberStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);
        return CompanyMemberResponse.from(companyMember);
    }

    // 직원 목록 조회
    public Page<CompanyMemberResponse> getCompanyMembers(UUID companyId, Pageable pageable) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);
        Page<CompanyMember> companyMembers = companyMemberRepository.findByCompanyIdAndStatus(companyId, CompanyMemberStatus.ACTIVE, pageable);
        return companyMembers.map(CompanyMemberResponse::from);
    }

    // 직원 삭제
    @Transactional
    public void deleteCompanyMember(UUID companyId, CompanyMemberDelRequest request, String companyManagerId) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);

        CompanyMember companyMember = companyMemberRepository.findByIdAndStatus(request.companyMemberId(), CompanyMemberStatus.ACTIVE)
                .orElseThrow(CompanyMemberNotFoundException::new);
        if (!companyMember.getCompany().getId().equals(companyId)) {
            throw new CompanyMemberNotFoundException();
        }

        ApiResponse<Void> updateUserResponse = userFeignClient.deleteUserOrClearCompany(companyMember.getUserId());
        if (updateUserResponse == null || !updateUserResponse.success()) {
            throw new UserSyncFailedException();
        }

        companyMember.delete(companyManagerId);
    }

    /**
     * 배송 생성시 입력받은 주소로 목적지 허브 조회 (Shipment ➡︎ Company) 단일요청
     */
    public CompanySearchHubResponse getHub(CompanySearchHubRequest request) {
        Company company = companyRepository.findByZipCodeAndAddressAndAddressDetail(request.zipCode(), request.address(), request.addressDetails())
                .orElseThrow(CompanyNotFoundException::new);

        return CompanySearchHubResponse.from(company.getHubId());
    }
}
