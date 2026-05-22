package com.sparta.whereismyparcel.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 전역적으로 사용되는 사용자 권한 Enum입니다.
 * GatewayHeaderAuthFilter 및 각 도메인 서비스의 권한 검증 시 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {
    MASTER("MASTER"),
    HUB_MANAGER("HUB_MANAGER"),
    DELIVERY_MANAGER("DELIVERY_MANAGER"),
    COMPANY_MANAGER("COMPANY_MANAGER");

    private final String roleName;

    public static UserRole fromString(String role) {
        for (UserRole userRole : UserRole.values()) {
            if (userRole.roleName.equalsIgnoreCase(role)) {
                return userRole;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 권한입니다: " + role);
    }
}
