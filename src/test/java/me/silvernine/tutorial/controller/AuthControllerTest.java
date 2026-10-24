package me.silvernine.tutorial.controller;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    private record Tokens(String access, String refresh) {
    }

    private void signup(String username) throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","nickname":"nick"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk());
    }

    private Tokens login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.token"), JsonPath.read(body, "$.refreshToken"));
    }

    private String refreshBody(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    @Test
    void signup_returnsUserWithoutPassword() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"%s","nickname":"nick"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.authorityDtoSet[0].authorityName").value("ROLE_USER"));
    }

    @Test
    void signup_invalidInput_returnsProblemDetailWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"a","password":"%s","nickname":"nick"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
    }

    @Test
    void signup_duplicate_returns409() throws Exception {
        signup("dup");
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"dup","password":"%s","nickname":"nick"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void authenticate_returnsAccessAndRefreshToken() throws Exception {
        signup("bob");
        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(header().string("Authorization", Matchers.startsWith("Bearer ")));
    }

    @Test
    void authenticate_wrongPassword_returns401() throws Exception {
        signup("mallory");
        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"mallory","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getMyUser_withoutToken_returns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getMyUser_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/user")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyUser_withValidToken_returns200() throws Exception {
        signup("carol");
        Tokens tokens = login("carol", PASSWORD);
        mockMvc.perform(get("/api/user")
                        .header("Authorization", "Bearer " + tokens.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("carol"));
    }

    @Test
    void adminEndpoint_withUserToken_returns403() throws Exception {
        signup("dave");
        Tokens tokens = login("dave", PASSWORD);
        mockMvc.perform(get("/api/user/dave")
                        .header("Authorization", "Bearer " + tokens.access()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminEndpoint_withAdminToken_returns200_and404ForUnknownUser() throws Exception {
        // data.sql 의 admin 계정 (비밀번호 admin)
        Tokens tokens = login("admin", "admin");
        mockMvc.perform(get("/api/user/user")
                        .header("Authorization", "Bearer " + tokens.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));
        mockMvc.perform(get("/api/user/nobody")
                        .header("Authorization", "Bearer " + tokens.access()))
                .andExpect(status().isNotFound());
    }

    @Test
    void refresh_rotatesTokens_andOldRefreshTokenStopsWorking() throws Exception {
        signup("erin");
        Tokens first = login("erin", PASSWORD);

        String body = mockMvc.perform(post("/api/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(first.refresh())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String newAccess = JsonPath.read(body, "$.token");
        String newRefresh = JsonPath.read(body, "$.refreshToken");
        assertThat(newRefresh).isNotEqualTo(first.refresh());

        mockMvc.perform(get("/api/user")
                        .header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk());

        // 한 번 쓴 Refresh Token 은 다시 쓸 수 없습니다.
        mockMvc.perform(post("/api/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(first.refresh())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        signup("frank");
        Tokens tokens = login("frank", PASSWORD);

        mockMvc.perform(post("/api/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(tokens.refresh())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(tokens.refresh())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_unknownToken_returns401() throws Exception {
        mockMvc.perform(post("/api/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody("not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }
}
