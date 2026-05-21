package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import com.sparta.whereismyparcel.shipment.domain.exception.DeliveryManagerCapacityExceededException;
import com.sparta.whereismyparcel.shipment.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.shipment.domain.exception.SlackIdNotFoundException;
import com.sparta.whereismyparcel.shipment.domain.repository.DeliveryManagerRepository;
import com.sparta.whereismyparcel.shipment.infrastructure.client.HubClient;
import com.sparta.whereismyparcel.shipment.infrastructure.client.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerPolicyTest {

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private HubClient hubClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DeliveryManagerPolicy deliveryManagerPolicy;

    private UUID hubId;
    private String slackId;

    @BeforeEach
    void setUp() {
        hubId = UUID.randomUUID();
        slackId = "slack-123";
    }

    @Nested
    @DisplayName("배송담당자 생성 검증")
    class create {

        @Test
        @DisplayName("Slack ID가 존재하지 않으면 예외가 발생한다")
        void checkCreate_fail_slackIdNotFound() {
            when(userClient.exists(slackId)).thenReturn(false);

            assertThrows(
                    SlackIdNotFoundException.class,
                    () -> deliveryManagerPolicy.checkCreate(
                            slackId,
                            DeliveryType.COMPANY_DELIVERY,
                            hubId
                    )
            );
        }

        @Test
        @DisplayName("업체 배송 담당자의 경우, 허브가 존재하지 않으면 예외가 발생한다")
        void checkCreate_fail_hubNotFound() {
            when(userClient.exists(slackId)).thenReturn(true);
            when(hubClient.exists(hubId)).thenReturn(false);

            assertThrows(
                    HubNotFoundException.class,
                    () -> deliveryManagerPolicy.checkCreate(
                            slackId,
                            DeliveryType.COMPANY_DELIVERY,
                            hubId
                    )
            );
        }

        @Test
        @DisplayName("배송 담당자 등록 시 최대 인원을 초과하면 예외가 발생한다")
        void checkCreate_fail_capacityExceeded() {
            when(userClient.exists(slackId)).thenReturn(true);
            when(hubClient.exists(hubId)).thenReturn(true);
            when(deliveryManagerRepository.countByHubId(hubId)).thenReturn(10L);

            assertThrows(
                    DeliveryManagerCapacityExceededException.class,
                    () -> deliveryManagerPolicy.checkCreate(
                            slackId,
                            DeliveryType.COMPANY_DELIVERY,
                            hubId
                    )
            );
        }

        @Test
        @DisplayName("Slack ID, 허브, 인원 제한 조건을 모두 만족하면 생성 정책 검증을 통과한다")
        void checkCreate_success() {
            when(userClient.exists(slackId)).thenReturn(true);
            when(hubClient.exists(hubId)).thenReturn(true);
            when(deliveryManagerRepository.countByHubId(hubId)).thenReturn(5L);

            assertDoesNotThrow(() ->
                    deliveryManagerPolicy.checkCreate(
                            slackId,
                            DeliveryType.COMPANY_DELIVERY,
                            hubId
                    )
            );
        }
    }

}
