package com.wind.security.jwt;

import org.springframework.security.oauth2.jwt.JwtException;

import java.io.Serial;

/**
 * jwt token expire
 *
 * @author wuxp
 * @date 2025-02-06 21:40
 **/
public class JwtExpiredException extends JwtException {

    @Serial
    private static final long serialVersionUID = -867678449082752815L;

    public JwtExpiredException(String message) {
        super(message);
    }

    public JwtExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
