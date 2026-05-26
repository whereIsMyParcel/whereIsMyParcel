package com.sparta.whereismyparcel.aislack.application.service;

import com.sparta.whereismyparcel.aislack.infrastructure.client.OrderFeignClient;
import com.sparta.whereismyparcel.aislack.infrastructure.client.ShipmentFeignClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiMessageService {

    private final OrderFeignClient orderFeignClient;
    private final ShipmentFeignClient shipmentFeignClient;

    @Transactional
}
