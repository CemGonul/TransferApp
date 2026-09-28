package com.transferapp.transferservice.exception;

public class IdempotencyKeyReuseException extends  RuntimeException{
    public IdempotencyKeyReuseException(String message){super(message);}
}
