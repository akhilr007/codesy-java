package com.codesy.platform.auth.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Service
public class RefreshTokenCookieService {

    private final String cookieName;
    private final Duration refreshExpiration;
    private final boolean secureCookie;
    private final String sameSite;
    private final String cookiePath;

    public RefreshTokenCookieService(@Value("${app.jwt.refresh-cookie-name}") String cookieName,
                                     @Value("${app.jwt.refresh-expiration}") Duration refreshExpiration,
                                     @Value("${app.jwt.refresh-cookie-secure}") boolean secureCookie,
                                     @Value("${app.jwt.refresh-cookie-same-site}") String sameSite,
                                     @Value("${app.jwt.refresh-cookie-path}") String cookiePath) {
        this.cookieName = cookieName;
        this.refreshExpiration = refreshExpiration;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.cookiePath = cookiePath;
    }

    public void writeRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(refreshToken).toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(cookieName, "")
                        .httpOnly(true)
                        .secure(secureCookie)
                        .sameSite(sameSite)
                        .path(cookiePath)
                        .maxAge(Duration.ZERO)
                        .build()
                        .toString()
        );
    }

    public String extractRefreshToken(HttpServletRequest request) {
        var cookie = WebUtils.getCookie(request, cookieName);
        return cookie != null ? cookie.getValue() : null;
    }

    public String getCookieName() {
        return cookieName;
    }

    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(refreshExpiration)
                .build();
    }
}