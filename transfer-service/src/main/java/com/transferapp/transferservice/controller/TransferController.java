package com.transferapp.transferservice.controller;

import com.transferapp.transferservice.dto.CreateTransferRequest;
import com.transferapp.transferservice.dto.PagedResponse;
import com.transferapp.transferservice.dto.TransferResponse;
import com.transferapp.transferservice.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTransferRequest request) {

        UUID authenticatedUserId =
                UUID.fromString(jwt.getSubject());

        TransferResponse response =
                transferService.createTransfer(
                        request,
                        authenticatedUserId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping
    public ResponseEntity<PagedResponse<TransferResponse>> getTransfers(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable){

        UUID userId=UUID.fromString(jwt.getSubject());

        Page<TransferResponse> responses=transferService.getTransfers(userId,pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(PagedResponse.from(responses));

    }
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> getTransfersById(
            @PathVariable UUID transferId,
            @AuthenticationPrincipal Jwt jwt){
        UUID userId=UUID.fromString(jwt.getSubject());

        TransferResponse response=transferService.getTransferById(transferId,userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
