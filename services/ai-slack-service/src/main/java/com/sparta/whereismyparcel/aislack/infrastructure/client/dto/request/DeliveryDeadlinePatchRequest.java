package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * order 서비스로 전송하는 최적 발송 시한 데이터를 위한 dto
 * @param finalDispatchDeadline
 */
public record DeliveryDeadlinePatchRequest(

        @NotNull(message = "최적 발송 시한은 필수입니다.")
        LocalDateTime finalDispatchDeadline
) {
}
