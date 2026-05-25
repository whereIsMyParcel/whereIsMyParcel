package com.sparta.whereismyparcel.company.domain.exception;

import com.sparta.whereismyparcel.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CompanyErrorCode implements ErrorCode {

    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY-001", "해당 업체를 찾을 수 없습니다."),

    HUB_NOT_FOUND(HttpStatus.NOT_FOUND,"COMPANY-002", "해당 허브를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"COMPANY-003", "해당 유저를 찾을 수 없습니다."),
    USER_SYNC_FAILED(HttpStatus.BAD_REQUEST, "COMPANY-004", "유저 서비스와의 소속 해제 동기화에 실패했습니다."),

    ALREADY_REGISTERED_MEMBER(HttpStatus.BAD_REQUEST, "COMPANY-005", "이미 등록된 직원입니다."),
    COMPANY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY-006", "해당 직원을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
