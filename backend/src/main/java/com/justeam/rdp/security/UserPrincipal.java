package com.justeam.rdp.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record UserPrincipal(
        long id,
        String username,
        String realName,
        String password,
        boolean enabled,
        boolean mustChangePassword,
        List<String> roles,
        List<String> permissions,
        Set<Long> assignedScopes,
        Set<Long> dataScopes
) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }

    public boolean admin() {
        return roles.contains("ADMIN");
    }
}
