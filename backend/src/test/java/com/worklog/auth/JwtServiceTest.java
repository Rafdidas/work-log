package com.worklog.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void blankSecret_generatesRandomKey_andStillIssuesValidTokens() {
        JwtService service = new JwtService("", Clock.systemUTC());

        assertThat(service.parse(service.issue(7))).isEqualTo(Optional.of(7L));
    }

    @Test
    void tokensFromAnotherKey_areRejected() {
        JwtService a = new JwtService("", Clock.systemUTC());
        JwtService b = new JwtService("", Clock.systemUTC());

        assertThat(b.parse(a.issue(7))).isEmpty();
    }

    @Test
    void configuredSecret_isStableAcrossInstances() {
        String secret = "a-configured-secret-that-is-at-least-32-bytes";
        JwtService a = new JwtService(secret, Clock.systemUTC());
        JwtService b = new JwtService(secret, Clock.systemUTC());

        assertThat(b.parse(a.issue(7))).isEqualTo(Optional.of(7L));
    }
}
