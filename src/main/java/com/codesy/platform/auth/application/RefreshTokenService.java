package com.codesy.platform.auth.application;

import com.codesy.platform.auth.domain.RefreshToken;
import com.codesy.platform.auth.infrastructure.RefreshTokenRepository;
import com.codesy.platform.shared.exception.UnauthorizedException;
import com.codesy.platform.user.domain.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String REASON_ROTATED = "ROTATED";
    private static final String REASON_LOGOUT = "LOGOUT";
    private static final String REASON_EXPIRED = "EXPIRED";
    private static final String REASON_REUSE_DETECTED = "REUSE_DETECTED";
    private static final String REASON_USER_DISABLED = "USER_DISABLED";

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${app.jwt.refresh-expiration}") Duration refreshExpiration) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
    }

    @Transactional
    public RefreshTokenSession issue(AppUser user, ClientRequestDetails requestDetails) {
        return createToken(user, UUID.randomUUID(), requestDetails, Instant.now());
    }

    @Transactional
    public RefreshTokenSession rotate(String rawRefreshToken, ClientRequestDetails requestDetails) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new UnauthorizedException("Refresh token is required");
        }

        Instant now = Instant.now();
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!currentToken.getUser().isEnabled()) {
            revokeFamily(currentToken.getFamilyId(), now, REASON_USER_DISABLED);
            throw new UnauthorizedException("User account is disabled");
        }

        if (currentToken.isRevoked()) {
            revokeFamily(currentToken.getFamilyId(), now, REASON_REUSE_DETECTED);
            throw new UnauthorizedException("Refresh token is no longer valid");
        }

        if (currentToken.isExpired(now)) {
            revoke(currentToken, now, REASON_EXPIRED, requestDetails, null);
            throw new UnauthorizedException("Refresh token has expired");
        }

        RefreshTokenSession replacement = createToken(
                currentToken.getUser(),
                currentToken.getFamilyId(),
                requestDetails,
                now
        );
        revoke(currentToken, now, REASON_ROTATED, requestDetails, hashToken(replacement.refreshToken()));
        currentToken.setLastUsedAt(now);
        currentToken.setLastUsedIp(requestDetails.ipAddress());
        refreshTokenRepository.save(currentToken);
        return replacement;
    }

    @Transactional
    public void revokeIfPresent(String rawRefreshToken, ClientRequestDetails requestDetails) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> {
                    revoke(token, Instant.now(), REASON_LOGOUT, requestDetails, null);
                    refreshTokenRepository.save(token);
                });
    }

    private RefreshTokenSession createToken(AppUser user,
                                            UUID familyId,
                                            ClientRequestDetails requestDetails,
                                            Instant now) {
        String rawToken = generateOpaqueToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setFamilyId(familyId);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(now.plus(refreshExpiration));
        refreshToken.setCreatedByIp(requestDetails.ipAddress());
        refreshToken.setLastUsedIp(requestDetails.ipAddress());
        refreshToken.setUserAgent(requestDetails.userAgent());
        refreshTokenRepository.save(refreshToken);  return new RefreshTokenSession(user, rawToken);
    }

    private void revokeFamily(UUID familyId, Instant now, String reason) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId);
        for (RefreshToken token : activeTokens) {
            token.setRevokedAt(now);
            token.setRevokedReason(reason);
            token.setLastUsedAt(now);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private void revoke(RefreshToken token,
                        Instant now,
                        String reason,
                        ClientRequestDetails requestDetails,
                        String replacementHash) {
        token.setRevokedAt(now);
        token.setRevokedReason(reason);
        token.setLastUsedAt(now);
        token.setLastUsedIp(requestDetails.ipAddress());
        token.setReplacedByTokenHash(replacementHash);
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = UUID.randomUUID().toString().concat(UUID.randomUUID().toString())
                .getBytes(StandardCharsets.UTF_8);
        return TOKEN_ENCODER.encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    public record RefreshTokenSession(AppUser user, String refreshToken) {}
}