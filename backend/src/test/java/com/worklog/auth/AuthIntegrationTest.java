package com.worklog.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.worklog.IntegrationTest;

import jakarta.servlet.http.Cookie;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void signup_thenMe_returnsProfileWithDefaultTeam() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(get("/api/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@example.com"))
                .andExpect(jsonPath("$.name").value("테스터"))
                .andExpect(jsonPath("$.teamLabel").value("개발1팀"));
    }

    @Test
    void signup_createsDefaultRecurringItems() throws Exception {
        login("a@example.com");

        List<Map<String, Object>> items = jdbc.queryForList(
                "select title, recurring, show_in_plan, status from work_items order by sort_order");
        assertThat(items).extracting(i -> i.get("title")).containsExactly("회의", "기타");
        assertThat(items).allSatisfy(i -> {
            assertThat(i.get("recurring")).isEqualTo(true);
            assertThat(i.get("show_in_plan")).isEqualTo(false);
            assertThat(i.get("status")).isEqualTo("IN_PROGRESS");
        });
    }

    @Test
    void login_wrongPassword_401() throws Exception {
        login("a@example.com");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_correctPassword_setsHttpOnlyCookie() throws Exception {
        login("a@example.com");

        String setCookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("WL_TOKEN=").contains("HttpOnly").contains("SameSite=Lax").contains("Path=/");
    }

    @Test
    void me_withoutCookie_401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void patchMe_updatesTeamLabel() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(patch("/api/me").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamLabel\":\"개발2팀\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamLabel").value("개발2팀"));
        mvc.perform(get("/api/me").cookie(cookie)).andExpect(jsonPath("$.teamLabel").value("개발2팀"));
    }

    @Test
    void signup_duplicateEmail_409() throws Exception {
        login("a@example.com");

        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"password\":\"password123\",\"name\":\"x\"}"))
                .andExpect(status().isConflict());
    }
}
