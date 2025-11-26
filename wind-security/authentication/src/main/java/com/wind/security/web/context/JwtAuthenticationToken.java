package com.wind.security.web.context;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * jwt token
 *
 * @author wuxp
 * @date 2023-10-26 12:44
 **/
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = -1650214530170210217L;

    private final transient Object principal;

    private final transient Object credentials;

    public JwtAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    public JwtAuthenticationToken(Object principal, Object credentials) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(false);
    }
}
