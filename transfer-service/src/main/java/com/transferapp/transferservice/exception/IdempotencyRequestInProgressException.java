package com.transferapp.transferservice.exception;

public class IdempotencyRequestInProgressException extends RuntimeException{
    public IdempotencyRequestInProgressException(String message){super(message);}
}
