package com.transferapp.transferservice.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String requiredAudience;

    public JwtAudienceValidator(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if(jwt.getAudience().contains(requiredAudience)){
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error=new OAuth2Error(
                "invalid_token",
                "Required audience is missing",
                null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
