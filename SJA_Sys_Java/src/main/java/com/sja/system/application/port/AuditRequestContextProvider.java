package com.sja.system.application.port;

import java.util.Optional;

/**
 * Supplies transport-neutral request metadata for an operation audit entry.
 */
public interface AuditRequestContextProvider {

    Optional<AuditRequestContext> currentContext();

    /**
     * Immutable request metadata. Application services do not need to know how it was obtained.
     */
    record AuditRequestContext(String requestId, String requestMethod, String requestUri,
                               String userAgent, String ipAddress, Long durationMs) {
    }
}
