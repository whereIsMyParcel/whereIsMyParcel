package com.sparta.whereismyparcel.shipment.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShipmentErrorCode implements ErrorCode {

    SLACK_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "SHIPMENT-001", "Slack ID가 존재하지 않습니다."),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "SHIPMENT-002", "해당 허브가 존재하지 않습니다."),
    DELIVERY_MANAGER_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "SHIPMENT-003", "등록 가능한 배송 담당자 수를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
