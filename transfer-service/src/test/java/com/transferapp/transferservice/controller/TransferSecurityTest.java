package com.transferapp.transferservice.controller;

import com.transferapp.transferservice.config.SecurityConfig;
import com.transferapp.transferservice.service.AccountService;
import com.transferapp.transferservice.service.RateLimitService;
import com.transferapp.transferservice.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AccountController.class, TransferController.class})
@Import(SecurityConfig.class)
class TransferSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void serviceRoleCannotAccessAccountsOrTransfers() throws Exception {
        for (String path : List.of("/accounts", "/transfers", "/transfers/" + UUID.randomUUID())) {
            mockMvc.perform(get(path)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE"))))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/transfers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountService, transferService, rateLimitService);
    }

    @Test
    void transferEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/transfers")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transferService, rateLimitService);
    }

    @Test
    void userRoleCanAccessOwnAccounts() throws Exception {
        UUID userId = UUID.randomUUID();
        when(accountService.getAccounts(userId)).thenReturn(List.of());

        mockMvc.perform(get("/accounts")
                        .with(jwt().jwt(token -> token.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());

        verify(accountService).getAccounts(userId);
    }

    @Test
    void userRoleCanAccessOwnTransferHistory() throws Exception {
        UUID userId = UUID.randomUUID();
        when(transferService.getTransfers(eq(userId), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/transfers")
                        .with(jwt().jwt(token -> token.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());

        verify(transferService).getTransfers(eq(userId), any(Pageable.class));
    }
}
