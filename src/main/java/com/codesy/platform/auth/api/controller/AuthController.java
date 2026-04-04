package com.codesy.platform.auth.api.controller;

import com.codesy.platform.auth.api.dto.AuthResponse;
import com.codesy.platform.auth.api.dto.LoginRequest;
import com.codesy.platform.auth.api.dto.RegisterRequest;
import com.codesy.platform.auth.application.AuthService;
import com.codesy.platform.auth.application.AuthSession;
import com.codesy.platform.auth.application.ClientRequestDetails;
import com.codesy.platform.auth.application.RefreshTokenCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
            ) {
        AuthSession session = authService.register(request, ClientRequestDetails.from(httpRequest));
        return buildAuthResponse(session, httpResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentication with username and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        AuthSession session = authService.login(request, ClientRequestDetails.from(httpRequest));
        return buildAuthResponse(session, httpResponse, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        String refreshToken = refreshTokenCookieService.extractRefreshToken(httpRequest);
        try {
            AuthSession session = authService.refresh(refreshToken, ClientRequestDetails.from(httpRequest));
            return buildAuthResponse(session, httpResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
           refreshTokenCookieService.clearRefreshTokenCookie(httpResponse);
           throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token and clear the auth cookie")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        String refreshToken = refreshTokenCookieService.extractRefreshToken(httpRequest);
        authService.logout(refreshToken, ClientRequestDetails.from(httpRequest));
        refreshTokenCookieService.clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noCache())
                .build();
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(AuthSession session,
                                                           HttpServletResponse httpResponse,
                                                           HttpStatus status) {
        refreshTokenCookieService.writeRefreshTokenCookie(session.refreshToken(), httpResponse);
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(session.response());
    }
}