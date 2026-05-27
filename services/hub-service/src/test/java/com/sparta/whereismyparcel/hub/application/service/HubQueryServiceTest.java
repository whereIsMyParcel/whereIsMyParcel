package com.sparta.whereismyparcel.hub.application.service;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HubQueryServiceTest {

    @InjectMocks
    private HubQueryService hubQueryService;

    @Mock
    private HubRepository hubRepository;

    @Test
    @DisplayName("ID로 허브 조회 성공")
    void getHub_Success() {
        UUID hubId = UUID.randomUUID();
        Hub hub = Hub.create("테스트 허브", "주소", 37.0, 127.0);
        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        HubResponse response = hubQueryService.getHub(hubId);

        assertThat(response.name()).isEqualTo("테스트 허브");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 예외 발생")
    void getHub_NotFound() {
        UUID hubId = UUID.randomUUID();
        given(hubRepository.findById(hubId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> hubQueryService.getHub(hubId))
                .isInstanceOf(HubNotFoundException.class);
    }
}
