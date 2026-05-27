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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HubCommandServiceTest {

    @InjectMocks
    private HubCommandService hubCommandService;

    @Mock
    private HubRepository hubRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("허브 생성 성공")
    void createHub_Success() {
        String name = "테스트 허브";
        String address = "서울시 어딘가";
        Double latitude = 37.0;
        Double longitude = 127.0;
        Hub hub = Hub.create(name, address, latitude, longitude);
        given(hubRepository.save(any(Hub.class))).willReturn(hub);

        HubResponse response = hubCommandService.createHub(name, address, latitude, longitude);

        assertThat(response.name()).isEqualTo(name);
        assertThat(response.address()).isEqualTo(address);
    }

    @Test
    @DisplayName("허브 수정 시 정보가 변경되고 캐시 삭제가 호출된다")
    void updateHub_Success() {
        UUID hubId = UUID.randomUUID();
        Hub hub = Hub.create("구 허브", "구 주소", 37.0, 127.0);
        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        HubResponse response = hubCommandService.updateHub(hubId, "신 허브", "신 주소", 38.0, 128.0);

        assertThat(response.name()).isEqualTo("신 허브");
        assertThat(hub.getName()).isEqualTo("신 허브");
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("허브 삭제 시 소프트 딜리트 처리 및 캐시 삭제가 호출된다")
    void deleteHub_Success() {
        UUID hubId = UUID.randomUUID();
        Hub hub = Hub.create("테스트 허브", "주소", 37.0, 127.0);
        given(hubRepository.findById(hubId)).willReturn(Optional.of(hub));

        hubCommandService.deleteHub(hubId, "user-123");

        assertThat(hub.isDeleted()).isTrue();
        assertThat(hub.getDeletedBy()).isEqualTo("user-123");
        verify(redisTemplate).execute(any(RedisCallback.class));
    }
}
