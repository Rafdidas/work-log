package com.worklog.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    public static final Duration TTL = Duration.ofDays(14);

    private final SecretKey key;
    private final Clock clock;

    public JwtService(@Value("${worklog.jwt.secret}") String secret, Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String issue(long userId) {
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(Date.from(clock.instant()))
                .expiration(Date.from(clock.instant().plus(TTL)))
                .signWith(key)
                .compact();
    }

    public Optional<Long> parse(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(Long.parseLong(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
