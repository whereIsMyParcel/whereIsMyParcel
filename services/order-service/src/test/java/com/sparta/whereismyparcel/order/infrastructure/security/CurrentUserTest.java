package com.sparta.whereismyparcel.order.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserIdAndRole() {
        // given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-1",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MASTER"))
                )
        );

        // when & then
        assertThat(CurrentUser.userId()).isEqualTo("user-1");
        assertThat(CurrentUser.role()).isEqualTo("MASTER");
    }
}
