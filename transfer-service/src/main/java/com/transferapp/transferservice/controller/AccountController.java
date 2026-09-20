package com.transferapp.transferservice.controller;


import com.transferapp.transferservice.dto.CreateAccountRequest;
import com.transferapp.transferservice.dto.CreateAccountResponse;
import com.transferapp.transferservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAccountRequest request){
        UUID userId=UUID.fromString(jwt.getSubject());
        CreateAccountResponse response=accountService.createAccount(request,userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
