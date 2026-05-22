package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import com.sparta.whereismyparcel.shipment.domain.repository.DeliveryManagerRepository;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerServiceTest {

    @Mock
    private DeliveryManagerPolicy deliveryManagerPolicy;

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @InjectMocks
    private DeliveryManagerService deliveryManagerService;

    @Test
    @DisplayName("배송 담당자 생성 요청 시 정책 검증 후 저장하고 응답 DTO를 반환한다")
    void create_success() {
        // given
        UUID hubId = UUID.randomUUID();
        String slackId = "slack-123";
        DeliveryType type = DeliveryType.COMPANY_DELIVERY;

        DeliveryManagerCreateRequest request =
                new DeliveryManagerCreateRequest(hubId, slackId, type);

        int nextOrder = 1;

        DeliveryManager savedEntity = DeliveryManager.create(
                hubId,
                slackId,
                type,
                nextOrder
        );

        when(deliveryManagerRepository.findNextOrderByCompany(hubId, type))
                .thenReturn(nextOrder);

        when(deliveryManagerRepository.save(any(DeliveryManager.class)))
                .thenReturn(savedEntity);

        // when
        DeliveryManagerCreateResponse response = deliveryManagerService.create(request);

        // then
        verify(deliveryManagerPolicy).checkCreate(slackId, type, hubId);

        verify(deliveryManagerRepository).findNextOrderByCompany(hubId, type);

        verify(deliveryManagerRepository).save(any(DeliveryManager.class));

        assertNotNull(response);
    }
}