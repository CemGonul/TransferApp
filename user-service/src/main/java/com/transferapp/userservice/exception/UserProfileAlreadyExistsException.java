package com.transferapp.userservice.exception;

public class UserProfileAlreadyExistsException extends RuntimeException{

    public UserProfileAlreadyExistsException(String message) {
        super(message);
    }
}
