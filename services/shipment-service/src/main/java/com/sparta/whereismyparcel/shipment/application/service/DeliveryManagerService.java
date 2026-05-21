package com.sparta.whereismyparcel.shipment.application.service;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.repository.DeliveryManagerRepository;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerService {

    private final DeliveryManagerPolicy deliveryManagerPolicy;
    private final DeliveryManagerRepository deliveryManagerRepository;

    @Transactional
    public DeliveryManagerCreateResponse create(DeliveryManagerCreateRequest request) {
        //1. 생성 정책 확인
        deliveryManagerPolicy.checkCreate(request.slackId(), request.type(), request.hubId());
        //2. 배송담당자 순번 채번
        int newOrder = deliveryManagerRepository.findNextDeliveryOrder(request.hubId(), request.type());
        //3. 저장
        DeliveryManager saved = deliveryManagerRepository.save(
                DeliveryManager.create(request.hubId(), request.slackId(), request.type(), newOrder)
        );

        return DeliveryManagerCreateResponse.from(saved);
    }

}
