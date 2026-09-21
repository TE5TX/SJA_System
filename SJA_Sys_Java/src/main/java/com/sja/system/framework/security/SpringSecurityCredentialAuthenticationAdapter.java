package com.sja.system.framework.security;

import com.sja.system.application.port.CredentialAuthenticationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapts Spring Security authentication to the application-layer credential port.
 */
@Component
@RequiredArgsConstructor
public class SpringSecurityCredentialAuthenticationAdapter implements CredentialAuthenticationPort {

    private final AuthenticationManager authenticationManager;

    @Override
    public Optional<String> authenticate(String loginIdentifier, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginIdentifier, password));
            return Optional.of(authentication.getName());
        } catch (AuthenticationException exception) {
            return Optional.empty();
        }
    }
}
