package com.transferapp.authservice.exception;

public class UserProfileProvisioningException extends RuntimeException {

    public UserProfileProvisioningException(String message) {
        super(message);
    }

    public UserProfileProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
