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
    DELIVERY_MANAGER_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "SHIPMENT-003", "등록 가능한 배송 담당자 수를 초과했습니다."),
    SHIPMENT_ALREADY_STARTED(HttpStatus.CONFLICT, "SHIPMENT-004", "배송을 시작하여 취소할 수 없습니다."),
    SHIPMENT_UPDATE_DENIED(HttpStatus.CONFLICT, "SHIPMENT-005", "배송 수정 권한이 없습니다."),
    SHIPMENT_CAN_NOT_DELIVERED(HttpStatus.CONFLICT, "SHIPMENT-006", "업체 이동 후, 배송 완료 가능합니다."),
    SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SHIPMENT-007", "배송이 존재하지 않습니다"),
    NO_AVAILABLE_DELIVERY_MANAGER(HttpStatus.CONFLICT, "SHIPMENT-008", "배정가능한 배송담당자가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
