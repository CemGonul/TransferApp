package com.transferapp.userservice.exception;



public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(String message) {
        super(message);
    }
}
