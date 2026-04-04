package com.codesy.platform.auth.application;

import com.codesy.platform.auth.api.dto.AuthResponse;
import com.codesy.platform.auth.api.dto.LoginRequest;
import com.codesy.platform.auth.application.RefreshTokenService.RefreshTokenSession;
import com.codesy.platform.auth.api.dto.RegisterRequest;
import com.codesy.platform.shared.exception.ConflictException;
import com.codesy.platform.shared.exception.UnauthorizedException;
import com.codesy.platform.user.domain.AppUser;
import com.codesy.platform.user.domain.UserRole;
import com.codesy.platform.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthSession register(RegisterRequest request, ClientRequestDetails requestDetails) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        AppUser user = new AppUser();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        userRepository.save(user);

        return createSession(user, requestDetails);
    }

    @Transactional
    public AuthSession login(LoginRequest request, ClientRequestDetails requestDetails) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Invalid username or password");
        }

        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        return createSession(user, requestDetails);
    }

    @Transactional
    public AuthSession refresh(String refreshToken, ClientRequestDetails requestDetails) {
        RefreshTokenSession refreshTokenSession = refreshTokenService.rotate(refreshToken, requestDetails);
        return createSessionResponse(refreshTokenSession.user(), refreshTokenSession.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken, ClientRequestDetails requestDetails) {
        refreshTokenService.revokeIfPresent(refreshToken, requestDetails);
    }

    private AuthSession createSession(AppUser user, ClientRequestDetails requestDetails) {
        RefreshTokenSession refreshTokenSession = refreshTokenService.issue(user, requestDetails);
        return createSessionResponse(user, refreshTokenSession.refreshToken());
    }

    private AuthSession createSessionResponse(AppUser user, String refreshToken) {
        JwtService.IssuedAccessToken accessToken =
                jwtService.issueAccessToken(user.getUsername(), user.getRole().name());
        AuthResponse response = new AuthResponse(
                accessToken.token(),
                "Bearer",
                accessToken.expiresAt(),
                user.getUsername(),
                user.getRole().name()
        );
        return new AuthSession(response, refreshToken);
    }
}