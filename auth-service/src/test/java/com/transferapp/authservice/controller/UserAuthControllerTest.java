package com.transferapp.authservice.controller;

import com.transferapp.authservice.config.SecurityConfig;
import com.transferapp.authservice.dto.LoginRequest;
import com.transferapp.authservice.dto.LoginResponse;
import com.transferapp.authservice.dto.RegisterRequest;
import com.transferapp.authservice.dto.RegisterResponse;
import com.transferapp.authservice.exception.DuplicateEmailException;
import com.transferapp.authservice.exception.GlobalExceptionHandler;
import com.transferapp.authservice.repository.UserAuthRepository;
import com.transferapp.authservice.service.LoginService;
import com.transferapp.authservice.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAuthService userAuthService;

    @MockitoBean
    private UserAuthRepository userAuthRepository;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void loginReturnsAccessToken() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "strongPassword12");
        when(loginService.login(request))
                .thenReturn(new LoginResponse("signed.jwt.token", "Bearer", 900));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "strongPassword12")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        verify(loginService).login(request);
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(loginService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Internal authentication detail"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "wrongPassword12")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Internal authentication detail"))));
    }

    @Test
    void loginRejectsInvalidEmailBeforeCallingService() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("invalid-email", "strongPassword12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(loginService);
    }

    @Test
    void loginRejectsEmailLongerThan254Characters() throws Exception {
        String oversizedEmail = "a".repeat(250) + "@x.com";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(oversizedEmail, "strongPassword12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(loginService);
    }

    @Test
    void loginRejectsPasswordLongerThan24Characters() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "a".repeat(25))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(loginService);
    }

    @Test
    void registerReturnsCreatedResponseWithoutPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("user@example.com", "strongPassword12");
        when(userAuthService.register(request))
                .thenReturn(new RegisterResponse("user@example.com", "Registration successful"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "strongPassword12"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(userAuthService).register(request);
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("invalid-email", "strongPassword12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.path").value("/auth/register"));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerRejectsEmailLongerThan254Characters() throws Exception {
        String oversizedEmail = "a".repeat(250) + "@x.com";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(oversizedEmail, "strongPassword12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerRejectsPasswordShorterThan12Characters() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("password")));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerRejectsPasswordLongerThan24Characters() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "a".repeat(25))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("password")));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerReturnsConflictForDuplicateEmail() throws Exception {
        when(userAuthService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("Email already in use"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "strongPassword12")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void registerReturnsConflictForDatabaseIntegrityViolation() throws Exception {
        when(userAuthService.register(any(RegisterRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Database constraint failed"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "strongPassword12")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DATA_INTEGRITY_VIOLATION"));
    }

    @Test
    void registerRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerRejectsMissingRequestBody() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerRejectsUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("payload"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerRejectsUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));

        verifyNoInteractions(userAuthService);
    }

    @Test
    void registerHidesUnexpectedExceptionDetails() throws Exception {
        when(userAuthService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalStateException("Sensitive internal detail"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("user@example.com", "strongPassword12")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Sensitive internal detail"))));
    }

    private String validRequestJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
