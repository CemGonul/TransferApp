package com.transferapp.transferservice.util;


import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final String PREFIX="TRF";
    private static final String LETTERS="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS="0123456789";
    private static final SecureRandom RANDOM=new SecureRandom();

    public String generate(){

        StringBuilder sb=new StringBuilder();

        sb.append(PREFIX).append("-");

        for(int i=0;i<4;i++){
            int index=RANDOM.nextInt(LETTERS.length());
            sb.append(LETTERS.charAt(index));
        }
        sb.append("-");

        for(int i=0;i<4;i++){
            int index=RANDOM.nextInt(DIGITS.length());
            sb.append(DIGITS.charAt(index));
        }
        return sb.toString();
    }
}
