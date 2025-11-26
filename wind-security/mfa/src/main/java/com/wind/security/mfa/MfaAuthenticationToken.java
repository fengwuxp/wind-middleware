package com.wind.security.mfa;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * @author wuxp
 * @date 2024-03-05 15:06
 **/
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MfaAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 7810089624358947844L;

    private final transient Object principal;

    private final transient Object credentials;

    public MfaAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    public MfaAuthenticationToken(Object principal, Object credentials) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(false);
    }
}
