package com.worklog.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.worklog.IntegrationTest;

@TestPropertySource(properties = "worklog.signup-enabled=false")
class SignupDisabledTest extends IntegrationTest {

    @Test
    void signupDisabled_403() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"password\":\"password123\",\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
