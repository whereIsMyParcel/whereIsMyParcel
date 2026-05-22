package com.sparta.whereismyparcel.company.application.service;

import com.sparta.whereismyparcel.company.domain.entity.Company;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMember;
import com.sparta.whereismyparcel.company.domain.entity.CompanyMemberStatus;
import com.sparta.whereismyparcel.company.domain.entity.CompanyStatus;
import com.sparta.whereismyparcel.company.domain.repository.CompanyMemberRepository;
import com.sparta.whereismyparcel.company.domain.repository.CompanyRepository;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyRegisterRequest;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyUpdateRequest;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyListResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyMemberResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyResponse;
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
    // user Feign client

    // 업체 등록
    @Transactional
    public CompanyResponse registerCompany(CompanyRegisterRequest request) {
        // TODO : 허브가 실제로 존재하는지 확인했다고 치고

        // TODO : 유저서비스에서 사업자 아이디 가져왔다고 치고
        String managerId =  UUID.randomUUID().toString();

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
        CompanyMember initialMember = CompanyMember.addMember(managerId, savedCompany);
        companyMemberRepository.save(initialMember);

        return CompanyResponse.from(savedCompany);
    }

    // 업체 조회
    public CompanyResponse getCompany(UUID companyId) {
        // TODO : 만약 허브 상태가 활성상태가 아닐시 검증 필요하면 작성


        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(()->new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));
        return CompanyResponse.from(company);
    }

    // 업체 목록 조회
    public Page<CompanyListResponse> getCompanies(Pageable pageable) {
        Page<Company> companies = companyRepository.findAllCompaniesByStatus(CompanyStatus.ACTIVE, pageable);

        return companies.map(CompanyListResponse::from);
    }

    // 업체 수정
    @Transactional
    public CompanyResponse updateCompanyDetails(UUID companyId,  CompanyUpdateRequest request) {
        Company company = companyRepository.findByCompanyIdAndStatus(companyId,CompanyStatus.ACTIVE)
                .orElseThrow(()-> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));

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
                .orElseThrow(()-> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));

        company.delete(hubManagerOrMasterId);
        // TODO : feign으로 유저에게 해당 컴퍼니 아이디를 가진 유저들의 컴퍼니 아이디를 지워달라고 요청 (아예 유저를 지워도 될것 같습니다)
    }

    // 업체 직원 등록
    @Transactional
    public CompanyMemberResponse registerCompanyMember (UUID companyId, String memberId) {
        // TODO : 유저에서 유저 정보를 가져옵니다
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(()-> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));

        // TODO : 다른 업체나 배송기사인지도 확인을 해야할까요??
        Boolean isExistsMember = companyMemberRepository.existsByUserId(memberId);
        if (isExistsMember) {
            throw new IllegalArgumentException("이미 등록된 직원입니다");
        }

        CompanyMember companyMember = CompanyMember.addMember(memberId, company);
        companyMemberRepository.save(companyMember);
        return CompanyMemberResponse.from(companyMember);
    }

    // 직원 조회
    public CompanyMemberResponse getCompanyMember(UUID companyId, UUID memberId) {
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(()-> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));
        CompanyMember companyMember = companyMemberRepository.findByCompanyMemberIdAndStatus(memberId, CompanyMemberStatus.ACTIVE)
                .orElseThrow(()-> new IllegalArgumentException("해당 직원을 찾을 수 없습니다"));
        return CompanyMemberResponse.from(companyMember);
    }

    // 직원 삭제
    @Transactional
    public void deleteCompanyMember(UUID companyId, UUID memberId, String companyManagerId) {
        Company company = companyRepository.findByCompanyIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(()-> new IllegalArgumentException("해당 업체를 찾을 수 없습니다"));

        CompanyMember companyMember = companyMemberRepository.findByCompanyMemberIdAndStatus(memberId, CompanyMemberStatus.ACTIVE)
                .orElseThrow(()-> new IllegalArgumentException("해당 직원을 찾을 수 없습니다"));

        // TODO : 유저 서비스에서 해당 유저의 업체아이디를 지워달라고 요청 (아예 유저를 지워도 될라나요?)
        companyMember.delete(companyManagerId);
    }
}
