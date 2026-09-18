package com.sja.system.application.port;

import java.util.Optional;

/**
 * Authenticates credentials without exposing Spring Security types to the application service.
 */
public interface CredentialAuthenticationPort {

    /**
     * @return the canonical username when authentication succeeds; otherwise empty
     */
    Optional<String> authenticate(String loginIdentifier, String password);
}
