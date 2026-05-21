package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import com.sparta.whereismyparcel.shipment.domain.exception.DeliveryManagerCapacityExceededException;
import com.sparta.whereismyparcel.shipment.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.shipment.domain.exception.SlackIdNotFoundException;
import com.sparta.whereismyparcel.shipment.domain.repository.DeliveryManagerRepository;
import com.sparta.whereismyparcel.shipment.infrastructure.client.HubClient;
import com.sparta.whereismyparcel.shipment.infrastructure.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryManagerPolicy {

    //배송담당자 등록 10명 제한
    private static final int MAX_DELIVERY_MANAGER_COUNT = 10;

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final HubClient hubClient;
    private final UserClient userClient;

    //등록 검증
    public void checkCreate(String slackId, DeliveryType type, UUID hubId) {
        validateSlackId(slackId);
        validateHub(type, hubId);
        checkLimit(type, hubId);
    }

    //slack id 존재하는지 확인
    private void validateSlackId(String slackId) {
        if (!userClient.exists(slackId)) {
            throw new SlackIdNotFoundException();
        }
    }

    //업체 담당 배송자의 경우, 존재하는 허브인지 확인
    private void validateHub(DeliveryType type, UUID hubId) {
        if (type == DeliveryType.HUB_DELIVERY) return;

        if (!hubClient.exists(hubId)) {
            throw new HubNotFoundException();
        }
    }

    //배송담당자 등록 10명 제한
    private void checkLimit(DeliveryType type, UUID hubId) {
        long managerCount = deliveryManagerRepository.countByHubIdAndType(hubId, type);

        if (managerCount < MAX_DELIVERY_MANAGER_COUNT) {
            return;
        }

        throw new DeliveryManagerCapacityExceededException();
    }
}
