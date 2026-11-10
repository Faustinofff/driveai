package com.driveai.controller;

import com.driveai.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** DEF-08: la clave de rate limit de IA no depende de datos controlados por el cliente. */
class AiAdvisorControllerTest {

    @Test
    void keyUsesAuthenticatedUserIdAndIgnoresForwardedHeader() {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader("X-Forwarded-For", "203.0.113.7");
        http.setRemoteAddr("10.0.0.5");

        String key = AiAdvisorController.rateLimitKey(
                new UserPrincipal(42L, "ana@driveai.test", "CUSTOMER"), http);

        assertThat(key).isEqualTo("user:42");
    }

    @Test
    void withoutPrincipalFallsBackToSocketAddressNotForwardedHeader() {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader("X-Forwarded-For", "203.0.113.7");
        http.setRemoteAddr("10.0.0.5");

        assertThat(AiAdvisorController.rateLimitKey(null, http)).isEqualTo("ip:10.0.0.5");
    }

    @Test
    void principalWithoutIdFallsBackToSocketAddress() {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setRemoteAddr("10.0.0.6");

        assertThat(AiAdvisorController.rateLimitKey(
                new UserPrincipal(null, "x@driveai.test", "CUSTOMER"), http)).isEqualTo("ip:10.0.0.6");
    }
}
