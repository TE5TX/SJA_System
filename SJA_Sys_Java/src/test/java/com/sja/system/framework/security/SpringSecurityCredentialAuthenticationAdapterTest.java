package com.sja.system.framework.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringSecurityCredentialAuthenticationAdapterTest {

    @Test
    void returnsTheCanonicalAuthenticatedUsername() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("20260001");
        when(manager.authenticate(any())).thenReturn(authentication);

        Optional<String> username = new SpringSecurityCredentialAuthenticationAdapter(manager)
                .authenticate("13800000000", "password");

        assertEquals(Optional.of("20260001"), username);
    }

    @Test
    void hidesSpringSecurityFailuresBehindAnEmptyResult() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("invalid"));

        Optional<String> username = new SpringSecurityCredentialAuthenticationAdapter(manager)
                .authenticate("student", "invalid");

        assertTrue(username.isEmpty());
    }
}
