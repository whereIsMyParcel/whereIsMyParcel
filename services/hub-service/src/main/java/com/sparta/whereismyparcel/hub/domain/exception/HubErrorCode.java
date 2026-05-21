package com.sparta.whereismyparcel.hub.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubErrorCode implements ErrorCode {
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB-001", "허브를 찾을 수 없습니다."),
    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB-002", "허브 간 이동 정보를 찾을 수 없습니다."),
    NO_PATH_BETWEEN_HUBS(HttpStatus.UNPROCESSABLE_ENTITY, "HUB-003", "출발 허브에서 목적지 허브까지 경로가 없습니다."),
    HUB_ALREADY_INACTIVE(HttpStatus.CONFLICT, "HUB-004", "이미 비활성화된 허브입니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "HUB-005", "페이지 크기는 10, 30, 50만 허용됩니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
