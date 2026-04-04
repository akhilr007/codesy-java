package com.codesy.platform.auth.application;

import com.codesy.platform.shared.exception.UnauthorizedException;
import com.codesy.platform.user.domain.AppUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken ||
            !(authentication.getPrincipal() instanceof AppUser user)) {

            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}