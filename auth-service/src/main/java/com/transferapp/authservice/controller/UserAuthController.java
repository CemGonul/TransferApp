package com.transferapp.authservice.controller;


import com.transferapp.authservice.dto.RegisterRequest;
import com.transferapp.authservice.dto.RegisterResponse;
import com.transferapp.authservice.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class UserAuthController {
    private final UserAuthService userAuthService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse registerResponse=userAuthService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registerResponse);
    }
}
