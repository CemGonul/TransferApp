package com.transferapp.transferservice.util;

import com.transferapp.transferservice.dto.CreateTransferRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RequestHashGenerator {

    private static final String CREATE_TRANSFER = "CREATE_TRANSFER";

    public String generate(CreateTransferRequest request) {

        String canonicalRequest = createCanonicalRequest(request);

        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = messageDigest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private String createCanonicalRequest(
            CreateTransferRequest request
    ) {

        String normalizedAccountNumber =
                request.targetAccountNumber().trim();

        BigDecimal normalizedAmount =
                request.amount().stripTrailingZeros();

        return CREATE_TRANSFER
                + "|sourceAccountId=" + request.sourceAccountId()
                + "|targetAccountNumber=" + normalizedAccountNumber
                + "|amount=" + normalizedAmount.toPlainString();
    }
}