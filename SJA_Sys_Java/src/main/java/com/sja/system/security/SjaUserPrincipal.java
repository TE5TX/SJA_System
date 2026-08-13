package com.sja.system.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class SjaUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final Integer highestRoleCode;
    private final Integer tokenVersion;
    private final Collection<? extends GrantedAuthority> authorities;

    public SjaUserPrincipal(Long userId, String username, String password,
                            Integer highestRoleCode, Integer tokenVersion,
                            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.highestRoleCode = highestRoleCode;
        this.tokenVersion = tokenVersion == null ? 0 : tokenVersion;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getHighestRoleCode() {
        return highestRoleCode;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
