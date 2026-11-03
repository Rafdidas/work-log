package com.worklog.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    public static final Duration TTL = Duration.ofDays(14);

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Clock clock;

    /** secret이 비어 있으면 실행할 때마다 무작위 키를 만든다(재시작하면 로그인이 풀린다). 저장소에 기본 비밀키를 두지 않기 위해서다. */
    public JwtService(@Value("${worklog.jwt.secret:}") String secret, Clock clock) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET이 설정되지 않아 무작위 키를 사용합니다. 재시작하면 로그인이 풀립니다.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
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
