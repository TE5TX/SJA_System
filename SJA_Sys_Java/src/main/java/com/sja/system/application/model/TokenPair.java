package com.sja.system.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Access and refresh tokens returned by the authentication use case.
 */
@Data
@AllArgsConstructor
public class TokenPair {
    private String accessToken;
    private String refreshToken;
}
