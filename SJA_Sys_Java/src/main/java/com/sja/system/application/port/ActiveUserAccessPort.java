package com.sja.system.application.port;

import java.util.Optional;
import java.util.Set;

/**
 * Provides the current access state of an active user without exposing a security-framework type.
 */
public interface ActiveUserAccessPort {

    /**
     * Reloads an active user and their latest authorities.
     *
     * @param userId user primary key
     * @return the latest access state, or empty when the account is absent or disabled
     */
    Optional<ActiveUserAccess> findActiveUserById(Long userId);

    /**
     * Immutable user and authorization data consumed by application services.
     */
    record ActiveUserAccess(Long userId, String username, String realName,
                            Integer highestRoleCode, Set<String> authorities) {

        public ActiveUserAccess {
            authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
        }

        public boolean hasAuthority(String authority) {
            return authorities.contains(authority);
        }
    }
}
