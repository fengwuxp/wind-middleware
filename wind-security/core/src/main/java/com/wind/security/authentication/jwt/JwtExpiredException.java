package com.wind.security.authentication.jwt;

import org.springframework.security.oauth2.jwt.JwtException;

/**
 * jwt token expire
 *
 * @author wuxp
 * @date 2025-02-06 21:40
 **/
public class JwtExpiredException extends JwtException {

    public JwtExpiredException(String message) {
        super(message);
    }

    public JwtExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
