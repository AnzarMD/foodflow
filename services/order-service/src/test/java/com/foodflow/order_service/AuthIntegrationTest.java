package com.foodflow.order_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.order_service.auth.dto.LoginRequest;
import com.foodflow.order_service.auth.dto.RegisterRequest;
import com.foodflow.order_service.auth.entity.User;
import com.foodflow.order_service.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
// ↑ Loads the full Spring application context — real beans, real DB, real security.
//   This is heavier than unit tests but tests the full stack integration.

@AutoConfigureMockMvc
// ↑ Configures MockMvc so we can make HTTP requests without starting a real server.
//   Requests go through the full filter chain (JWT, security, etc.)

@ActiveProfiles("test")
// ↑ Activates application-test.yml instead of application.yml

@Transactional
// ↑ Each test runs in a transaction that is rolled back after the test.
//   This keeps tests isolated — data created in one test doesn't affect others.

public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "integration-test@foodflow.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NAME = "Integration Test User";

    @BeforeEach
    void cleanUp() {
        // Remove the test user if it exists from a previous run (in case @Transactional
        // didn't roll back due to a previous failure)
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void register_shouldReturn201_withTokens() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        request.setFullName(TEST_NAME);
        request.setRole(User.Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // ↑ 201 Created
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                // ↑ Response must contain a non-null accessToken
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void register_duplicateEmail_shouldReturn400() throws Exception {
        // Register once
        RegisterRequest request = new RegisterRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        request.setFullName(TEST_NAME);
        request.setRole(User.Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to register again with same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void login_validCredentials_shouldReturn200() throws Exception {
        // First register
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setEmail(TEST_EMAIL);
        regRequest.setPassword(TEST_PASSWORD);
        regRequest.setFullName(TEST_NAME);
        regRequest.setRole(User.Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_wrongPassword_shouldReturn401() throws Exception {
        // Register first
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setEmail(TEST_EMAIL);
        regRequest.setPassword(TEST_PASSWORD);
        regRequest.setFullName(TEST_NAME);
        regRequest.setRole(User.Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void login_nonExistentUser_shouldReturn401() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nobody@foodflow.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_missingFields_shouldReturn400() throws Exception {
        // Empty body
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}