package com.codesy.platform.user.application;

import com.codesy.platform.auth.application.AuthenticatedUserProvider;
import com.codesy.platform.user.api.dto.UserResponse;
import com.codesy.platform.user.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserResponse currentUser() {
        AppUser user = authenticatedUserProvider.getCurrentUser();
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }
}