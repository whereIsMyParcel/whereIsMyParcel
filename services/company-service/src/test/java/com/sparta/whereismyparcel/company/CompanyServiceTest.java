package com.sparta.whereismyparcel.company;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.company.application.service.CompanyService;
import com.sparta.whereismyparcel.company.domain.entity.*;
import com.sparta.whereismyparcel.company.domain.exception.*;
import com.sparta.whereismyparcel.company.domain.repository.CompanyMemberRepository;
import com.sparta.whereismyparcel.company.domain.repository.CompanyRepository;
import com.sparta.whereismyparcel.company.infrastructure.feign.client.HubFeignClient;
import com.sparta.whereismyparcel.company.infrastructure.feign.client.UserFeignClient;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyMemberRequest;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanyRegisterRequest;
import com.sparta.whereismyparcel.company.presentation.dto.request.CompanySearchHubRequest;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanyMemberResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.CompanySearchHubResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.UserIdResponse;
import com.sparta.whereismyparcel.company.domain.exception.HubNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CompanyMemberRepository companyMemberRepository;
    @Mock
    UserFeignClient userFeignClient;
    @Mock
    HubFeignClient hubFeignClient;

    @InjectMocks
    private CompanyService companyService;

    private CompanyRegisterRequest createRegisterRequest() {
        return new CompanyRegisterRequest(
                UUID.randomUUID(),
                CompanyType.RECEIVER,
                "스파르타물류(주)",
                "111-11-11111",
                "김라탄",
                "010-1267-5652",
                "12345",
                "서울특별시 강남구 개포동",
                "백엔드 빌딩 10층"
        );
    }

    private Company createCompany() {
        return Company.create(
                UUID.randomUUID(),
                com.sparta.whereismyparcel.company.domain.entity.CompanyType.RECEIVER,
                "스파르타물류",
                "111-11-11111",
                "김라탄",
                "010-1111-2222",
                "12345",
                "서울시",
                "101호"
        );
    }

    @Test
    @DisplayName("업체 등록 성공 - 올바른 데이터를 입력하면 유저 ID를 조회하고 업체를 생성한다")
    void registerCompanySuccess() {
        // given
        CompanyRegisterRequest request = createRegisterRequest();
        UUID mockUserId = UUID.randomUUID();

        given(companyRepository.existsByBusinessNumber(request.businessNumber())).willReturn(false);
        given(companyRepository.existsByCompanyName(request.companyName())).willReturn(false);

        // 2. 가상 허브 서비스 응답 설정 (허브가 진짜 존재한다고 가정)
        given(hubFeignClient.isHubExists(request.hubId())).willReturn(ApiResponse.success(true));

        // 3. 가상 유저 서비스 응답 설정 (사업자번호로 가짜 유저 ID 리턴)
        given(userFeignClient.getUserIdByBusinessNumber(request.businessNumber()))
                .willReturn(ApiResponse.success(new UserIdResponse(mockUserId)));

        // 4. 레포지토리 save 호출 시 생성된 컴퍼니 객체를 그대로 반환하도록 설정
        given(companyRepository.save(any(Company.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // 5. 유저의 회사 ID 업데이트 Feign 통신 성공 설정
        given(userFeignClient.updateUserCompanyId(eq(mockUserId), any()))
                .willReturn(ApiResponse.success(null));


        // when
        var response = companyService.registerCompany(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.companyName()).isEqualTo(request.companyName());
        assertThat(response.businessNumber()).isEqualTo(request.businessNumber());

        then(userFeignClient).should().getUserIdByBusinessNumber(request.businessNumber());
        then(userFeignClient).should().updateUserCompanyId(eq(mockUserId), any());
        then(companyMemberRepository).should().save(any());
    }


    @Test
    @DisplayName("업체 등록 실패 - 이미 존재하는 사업자 번호면 예외가 터진다")
    void registerCompanyFailDuplicateBusinessNumber() {
        // given
        CompanyRegisterRequest request = createRegisterRequest();

        given(companyRepository.existsByBusinessNumber(request.businessNumber())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> companyService.registerCompany(request))
                .isInstanceOf(BusinessNumberIsExistsException.class);


       then(hubFeignClient).should(never()).isHubExists(any());
    }

    @Test
    @DisplayName("업체 등록 실패 - 이미 존재하는 업체 이름이면 예외가 터진다")
    void registerCompanyFailDuplicateCompanyName() {
        // given
        CompanyRegisterRequest request = createRegisterRequest();
        given(companyRepository.existsByBusinessNumber(request.businessNumber())).willReturn(false);

        given(companyRepository.existsByCompanyName(request.companyName())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> companyService.registerCompany(request))
                .isInstanceOf(CompanyNameIsExistsException.class);
    }

    @Test
    @DisplayName("업체 등록 실패 - 매핑된 허브가 존재하지 않으면 예외가 터진다")
    void registerCompanyFailHubNotFound() {
        // given
        CompanyRegisterRequest request = createRegisterRequest();
        given(companyRepository.existsByBusinessNumber(request.businessNumber())).willReturn(false);
        given(companyRepository.existsByCompanyName(request.companyName())).willReturn(false);

        given(hubFeignClient.isHubExists(request.hubId())).willReturn(ApiResponse.success(false));

        // when & then
        assertThatThrownBy(() -> companyService.registerCompany(request))
                .isInstanceOf(HubNotFoundException.class);
    }

    @Test
    @DisplayName("업체 등록 실패 - 사업자번호와 매핑된 유저가 없으면 예외가 터진다")
    void registerCompanyFailUserNotFound() {
        // given
        CompanyRegisterRequest request = createRegisterRequest();
        given(companyRepository.existsByBusinessNumber(request.businessNumber())).willReturn(false);
        given(companyRepository.existsByCompanyName(request.companyName())).willReturn(false);
        given(hubFeignClient.isHubExists(request.hubId())).willReturn(ApiResponse.success(true));


        given(userFeignClient.getUserIdByBusinessNumber(request.businessNumber()))
                .willReturn(ApiResponse.success(null));

        // when & then
        assertThatThrownBy(() -> companyService.registerCompany(request))
                .isInstanceOf(UserNotFoundException.class);

        then(companyRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("업체 삭제 성공 - 존재하는 업체라면 ACTIVE 상태를 지우고 유저 동기화 탈퇴를 연쇄 호출한다")
    void deleteCompanySuccess() {
        // given
        UUID companyId = UUID.randomUUID();
        String hubManagerOrMasterId = "MASTER_USER_1";
        Company company = createCompany();

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(company));
        given(userFeignClient.deleteAllUsersInCompany(companyId)).willReturn(ApiResponse.success(null));

        // when
        companyService.deleteCompany(companyId, hubManagerOrMasterId);

        // then
        then(userFeignClient).should().deleteAllUsersInCompany(companyId);
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.INACTIVE);
    }

    @Test
    @DisplayName("업체 삭제 실패 - 존재하지 않거나 이미 삭제된 업체 ID면 CompanyNotFoundException이 터진다")
    void deleteCompanyFailCompanyNotFound() {
        // given
        UUID companyId = UUID.randomUUID();

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> companyService.deleteCompany(companyId, "MASTER_ID"))
                .isInstanceOf(CompanyNotFoundException.class);

        then(userFeignClient).should(never()).deleteAllUsersInCompany(any());
    }

    @Test
    @DisplayName("업체 삭제 실패 - 유저 삭제 Feign 통신이 실패하면 UserSyncFailedException이 터진다")
    void deleteCompanyFailUserSyncFailed() {
        // given
        UUID companyId = UUID.randomUUID();
        Company company = createCompany();

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(company));
        given(userFeignClient.deleteAllUsersInCompany(companyId)).willReturn(ApiResponse.success(null));
        given(userFeignClient.deleteAllUsersInCompany(companyId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> companyService.deleteCompany(companyId, "MASTER_ID"))
                .isInstanceOf(UserSyncFailedException.class);
    }

    @Test
    @DisplayName("직원 등록 성공 - 유효한 업체이고 중복되지 않은 유저라면 직원을 등록하고 유저 서비스를 업데이트한다")
    void registerCompanyMemberSuccess() {
        // given
        UUID companyId = UUID.randomUUID();
        Company company = createCompany();
        UUID memberUserId = UUID.randomUUID();
        CompanyMemberRequest request = new CompanyMemberRequest(memberUserId);


        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(company));
        given(companyMemberRepository.existsByUserId(memberUserId)).willReturn(false);
        given(userFeignClient.updateUserCompanyId(memberUserId, companyId)).willReturn(ApiResponse.success(null));

        // when
        CompanyMemberResponse response = companyService.registerCompanyMember(companyId, request);

        // then
        assertThat(response).isNotNull();
        then(companyMemberRepository).should().save(any(CompanyMember.class));
    }

    @Test
    @DisplayName("직원 등록 실패 - 존재하지 않거나 ACTIVE가 아닌 업체 ID면 CompanyNotFoundException이 터진다")
    void registerCompanyMemberFailCompanyNotFound() {
        // given
        UUID companyId = UUID.randomUUID();
        CompanyMemberRequest request = new CompanyMemberRequest(UUID.randomUUID());

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> companyService.registerCompanyMember(companyId, request))
                .isInstanceOf(CompanyNotFoundException.class);

        then(companyMemberRepository).should(never()).existsByUserId(any());
        then(userFeignClient).should(never()).updateUserCompanyId(any(), any());
    }

    @Test
    @DisplayName("직원 등록 실패 - 이미 소속이 등록되어 있는 유저 아이디면 AlreadyRegisterMemberException이 터진다")
    void registerCompanyMemberFailAlreadyRegistered() {
        // given
        UUID companyId = UUID.randomUUID();
        Company company = createCompany();
        UUID memberUserId = UUID.randomUUID();
        CompanyMemberRequest request = new CompanyMemberRequest(memberUserId);

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(company));
        given(companyMemberRepository.existsByUserId(memberUserId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> companyService.registerCompanyMember(companyId, request))
                .isInstanceOf(AlreadyRegisterMemberException.class);

        then(userFeignClient).should(never()).updateUserCompanyId(any(), any());
    }

    @Test
    @DisplayName("직원 등록 실패 - 유저 서비스 연동(Feign) 업데이트가 실패하면 UserSyncFailedException이 터진다")
    void registerCompanyMemberFailUserSyncFailed() {
        // given
        UUID companyId = UUID.randomUUID();
        Company company = createCompany();
        UUID memberUserId = UUID.randomUUID();
        CompanyMemberRequest request = new CompanyMemberRequest(memberUserId);

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(company));
        given(companyMemberRepository.existsByUserId(memberUserId)).willReturn(false);

        given(userFeignClient.updateUserCompanyId(memberUserId, companyId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> companyService.registerCompanyMember(companyId, request))
                .isInstanceOf(UserSyncFailedException.class);

        then(companyMemberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("직원 삭제 성공 - 유효한 업체이고 소속 직원이 맞다면 탈퇴 처리 후 유저 상태를 클리어 링킹한다")
    void deleteCompanyMemberSuccess() {
        // given
        UUID companyId = UUID.randomUUID();

        Company company = spy(createCompany());
        given(company.getId()).willReturn(companyId);

        CompanyMember companyMember = CompanyMember.addMember(UUID.randomUUID(), company);
        CompanyMemberRequest request = new CompanyMemberRequest(companyMember.getUserId());

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(company));
        given(companyMemberRepository.findByIdAndStatus(request.memberUserId(), CompanyMemberStatus.ACTIVE))
                .willReturn(Optional.of(companyMember));
        given(userFeignClient.deleteUserOrClearCompany(companyMember.getUserId())).willReturn(ApiResponse.success(null));

        // when
        companyService.deleteCompanyMember(companyId, request, "MANAGER_ID");

        // then
        then(userFeignClient).should().deleteUserOrClearCompany(companyMember.getUserId());
    }

    @Test
    @DisplayName("직원 삭제 실패 - 존재하지 않거나 ACTIVE가 아닌 업체 ID면 CompanyNotFoundException이 터진다")
    void deleteCompanyMemberFailCompanyNotFound() {
        // given
        UUID companyId = UUID.randomUUID();
        CompanyMemberRequest request = new CompanyMemberRequest(UUID.randomUUID());

        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> companyService.deleteCompanyMember(companyId, request, "MANAGER_ID"))
                .isInstanceOf(CompanyNotFoundException.class);

        then(companyMemberRepository).should(never()).findByIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("직원 삭제 실패 - 삭제하려는 직원이 해당 업체 소속이 아니라면 CompanyMemberNotFoundException이 터진다")
    void deleteCompanyMemberFailCompanyMismatch() {
        // given
        UUID requestCompanyId = UUID.randomUUID();
        UUID actualCompanyId = UUID.randomUUID();

        Company otherCompany = spy(createCompany());
        given(otherCompany.getId()).willReturn(actualCompanyId);

        CompanyMember companyMember = CompanyMember.addMember(UUID.randomUUID(), otherCompany);
        CompanyMemberRequest request = new CompanyMemberRequest(companyMember.getUserId());

        given(companyRepository.findByIdAndStatus(requestCompanyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(createCompany()));
        given(companyMemberRepository.findByIdAndStatus(request.memberUserId(), CompanyMemberStatus.ACTIVE))
                .willReturn(Optional.of(companyMember));

        // when & then
        assertThatThrownBy(() -> companyService.deleteCompanyMember(requestCompanyId, request, "MANAGER_ID"))
                .isInstanceOf(CompanyMemberNotFoundException.class);

        then(userFeignClient).should(never()).deleteUserOrClearCompany(any());
    }

    @Test
    @DisplayName("직원 삭제 실패 - 유저 삭제 연동 Feign 통신이 실패하면 UserSyncFailedException이 터진다")
    void deleteCompanyMemberFailUserSyncFailed() {
        // given
        UUID companyId = UUID.randomUUID();

        Company company = spy(createCompany());
        given(company.getId()).willReturn(companyId);

        CompanyMember companyMember = CompanyMember.addMember(UUID.randomUUID(), company);
        CompanyMemberRequest request = new CompanyMemberRequest(companyMember.getUserId());


        given(companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).willReturn(Optional.of(company));
        given(companyMemberRepository.findByIdAndStatus(request.memberUserId(), CompanyMemberStatus.ACTIVE))
                .willReturn(Optional.of(companyMember));

        given(userFeignClient.deleteUserOrClearCompany(companyMember.getUserId())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> companyService.deleteCompanyMember(companyId, request, "MANAGER_ID"))
                .isInstanceOf(UserSyncFailedException.class);
    }

    @Test
    @DisplayName("목적지 허브 조회 성공 - 주소 3종 세트가 정확히 일치하면 매핑된 허브 ID를 정상 반환한다")
    void getHubSuccess() {
        // given
        CompanySearchHubRequest request =
                new CompanySearchHubRequest("12345", "서울시", "101호");

        Company company = createCompany();
        UUID expectedHubId = company.getHubId();

        given(companyRepository.findByZipCodeAndAddressAndAddressDetail(request.zipCode(), request.address(), request.addressDetails()))
                .willReturn(Optional.of(company));

        // when
        CompanySearchHubResponse response = companyService.getHub(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.hubId()).isEqualTo(expectedHubId);
    }

    @Test
    @DisplayName("목적지 허브 조회 실패 - 일치하는 주소의 업체가 없다면 CompanyNotFoundException이 터진다")
    void getHubFailCompanyNotFound() {
        // given
        CompanySearchHubRequest request =
                new CompanySearchHubRequest("99999", "엉뚱한 주소", "없는 방");

        given(companyRepository.findByZipCodeAndAddressAndAddressDetail(request.zipCode(), request.address(), request.addressDetails()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> companyService.getHub(request))
                .isInstanceOf(CompanyNotFoundException.class);
    }
}
