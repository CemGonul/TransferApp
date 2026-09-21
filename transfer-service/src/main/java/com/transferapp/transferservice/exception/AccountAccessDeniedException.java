package com.transferapp.transferservice.exception;

public class AccountAccessDeniedException extends RuntimeException{
    public AccountAccessDeniedException(String message){super(message);}
}
