package com.sparta.whereismyparcel.inventory.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InventoryErrorCode implements ErrorCode {


    NOT_ENOUGH_AVAILABLE_STOCK(HttpStatus.BAD_REQUEST, "INVENTORY-001", "주문 가능한 가용 재고가 부족합니다."),
    INVALID_MINUS_RESERVED_STOCK(HttpStatus.BAD_REQUEST, "INVENTORY-002", "차감하려는 예약 재고가 기존 예약 수량보다 많습니다."),
    INVALID_MINUS_PHYSICAL_STOCK(HttpStatus.BAD_REQUEST, "INVENTORY-003", "출고하려는 수량이 현재 창고 물리 재고보다 많습니다."),
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "INVENTORY-004", "해당 허브의 재고 정보를 찾을 수 없습니다."),
    INVENTORY_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "INVENTORY-005", "해당 허브에 이미 등록된 상품 재고입니다."),

    HUB_NOT_FOUND(HttpStatus.NOT_FOUND,"INVENTORY-006", "해당 허브를 찾을 수 없습니다."),

    PRODUCT_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "INVENTORY-007", "해당 옵션 조합을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
