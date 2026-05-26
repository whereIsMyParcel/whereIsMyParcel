package com.sparta.whereismyparcel.company.application.service;

import com.sparta.whereismyparcel.common.dto.HubValidateRequest;
import com.sparta.whereismyparcel.common.infrastructure.client.HubFeignClient;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.company.domain.entity.Company;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMember;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMemberStatus;
import com.sparta.whereismyparcel.company.domain.entity.CompanyStatus;
import com.sparta.whereismyparcel.company.domain.exception.*;
import com.sparta.whereismyparcel.company.domain.repository.CompanyMemberRepository;
import com.sparta.whereismyparcel.company.domain.repository.CompanyRepository;
import com.sparta.whereismyparcel.company.infrastructure.feign.client.UserFeignClient;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyMemberRequest;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyRegisterRequest;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyUpdateRequest;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyListResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyMemberResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.UserIdResponse;
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
        // 해당 허브가 존재하는지 검증
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

        // TODO : 리퀘스트를 만들어서 등록할 유저의 아이디를 입력을 하고 유저서비스에 해당 아이디의 유저에 컴퍼니아이디를 내가 주는걸로 넣어라
        ApiResponse<Void> updateUserResponse = userFeignClient.updateUserCompanyId(managerId, savedCompany.getCompanyId());
        if (updateUserResponse == null || !updateUserResponse.success()) {
            throw new UserSyncFailedException();
        }
        CompanyMember initialMember = CompanyMember.addMember(managerId, savedCompany);
        companyMemberRepository.save(initialMember);

        return CompanyResponse.from(savedCompany);
    }

    // 업체 조회
    public CompanyResponse getCompany(UUID companyId) {
        // TODO : 만약 허브 상태가 활성상태가 아닐시 검증 필요하면 작성

        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));
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
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));

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
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);

        company.delete(hubManagerOrMasterId);
        // TODO : feign으로 유저에게 해당 컴퍼니 아이디를 가진 유저들의 컴퍼니 아이디를 지워달라고 요청 (아예 유저를 지워도 될것 같습니다)
        ApiResponse<Void> userDeleteResponse = userFeignClient.deleteAllUsersInCompany(companyId);

        // 4. 유저 서비스 쪽에서 처리하다가 에러가 났거나 통신 실패 시 방어 코드
        if (userDeleteResponse == null || !userDeleteResponse.success()) { // 또는 .isSuccess() 등 프로젝트 공통 포맷에 맞춤
            throw new UserNotFoundException();
        }
    }

    // 업체 직원 등록
    @Transactional
    public CompanyMemberResponse registerCompanyMember(UUID companyId, CompanyMemberRequest request) {
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);

        Boolean isExistsMember = companyMemberRepository.existsByUserId(request.userId());
        if (isExistsMember) {
            throw new AlreadyRegisterMemberException();
        }

        // TODO : 리퀘스트를 만들어서 등록할 유저의 아이디를 입력을 하고 유저서비스에 해당 아이디의 유저에 컴퍼니아이디를 내가 주는걸로 넣어라
        ApiResponse<Void> updateUserResponse = userFeignClient.updateUserCompanyId(request.userId(), companyId);
        if (updateUserResponse == null || !updateUserResponse.success()) {
            throw new UserSyncFailedException();
        }

        CompanyMember companyMember = CompanyMember.addMember(request.userId(), company);
        companyMemberRepository.save(companyMember);
        return CompanyMemberResponse.from(companyMember);
    }

    // 직원 조회
    public CompanyMemberResponse getCompanyMember(UUID companyId, UUID memberId) {
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));
        CompanyMember companyMember = companyMemberRepository.findByCompanyMemberIdAndStatus(memberId, CompanyMemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원을 찾을 수 없습니다"));
        return CompanyMemberResponse.from(companyMember);
    }

    // 직원 삭제
    @Transactional
    public void deleteCompanyMember(UUID companyId, CompanyMemberRequest request, String companyManagerId) {
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(CompanyNotFoundException::new);

        CompanyMember companyMember = companyMemberRepository.findByCompanyMemberIdAndStatus(request.userId(), CompanyMemberStatus.ACTIVE)
                .orElseThrow(CompanyMemberNotFoundException::new);

        // TODO : 유저 서비스에서 해당 유저를 지워달라고 요청
        ApiResponse<Void> updateUserResponse = userFeignClient.deleteUserOrClearCompany(request.userId());
        if (updateUserResponse == null || !updateUserResponse.success()) {
            throw new UserSyncFailedException();
        }

        companyMember.delete(companyManagerId);
    }
}
