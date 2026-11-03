package com.worklog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTest.TestClockConfig.class)
public abstract class IntegrationTest {

    /** 2026-11-04 (수) 10:00 KST */
    public static final Instant DEFAULT_NOW = Instant.parse("2026-11-04T01:00:00Z");

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    public static class TestClock extends Clock {
        private volatile Instant now = DEFAULT_NOW;

        public void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Seoul");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected TestClock clock;

    @BeforeEach
    void resetState() {
        clock.set(DEFAULT_NOW);
        jdbc.execute("truncate table day_entries, day_logs, work_items, users, teams restart identity cascade");
    }

    /** 가입 후 로그인 쿠키를 돌려준다. 비밀번호는 항상 password123. */
    protected Cookie login(String email) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"테스터\"}";
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getCookie("WL_TOKEN");
    }
}
