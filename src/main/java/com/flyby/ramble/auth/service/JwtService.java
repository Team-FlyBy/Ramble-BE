package com.flyby.ramble.auth.service;

import com.flyby.ramble.auth.model.RefreshToken;
import com.flyby.ramble.auth.repository.RefreshTokenRepository;
import com.flyby.ramble.auth.util.CookieUtil;
import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    // TODO: DeviceType 구분 로직 필요, 현재는 WEB으로 고정
    public void generateTokens(User user, HttpServletResponse response) {
        String accToken  = jwtUtil.generateAccToken(user.getExternalId().toString(), user.getRole(), DeviceType.WEB, user.getProvider(), user.getProviderId());
        String refToken  = jwtUtil.generateRefToken(user.getExternalId().toString(), user.getRole(), DeviceType.WEB, user.getProvider(), user.getProviderId());
        Claims refClaims = parseRefreshToken(refToken);

        refreshTokenRepository.save(createRefreshToken(user, refClaims));

        populateResponse(accToken, refToken, response);
    }

    public void reissueTokens(HttpServletRequest request, HttpServletResponse response) {
        String refToken  = cookieUtil.getCookie(request).orElseThrow(() -> new BaseException(ErrorCode.INVALID_REFRESH_TOKEN));
        Claims refClaims = parseRefreshToken(refToken);
        String jti       = refClaims.get("jti", String.class);
        UUID   userId    = UUID.fromString(refClaims.getSubject());
        DeviceType type  = DeviceType.valueOf(refClaims.get("deviceType", String.class));

        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByIdAndRevokedFalse(jti);

        if (optionalToken.isEmpty()) {
            log.warn("Invalid refresh token used: {}, userId: {}, deviceType: {}", jti, userId, type);
            refreshTokenRepository.revokeAllByUserIdAndDeviceType(userId, type);
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }

        RefreshToken refreshToken = optionalToken.get();
        refreshTokenRepository.save(refreshToken.revoke());
        generateTokens(refreshToken.getUser(), response);
    }

    private Claims parseRefreshToken(String refreshToken) {
        try {
            return jwtUtil.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private RefreshToken createRefreshToken(User user, Claims claims) {
        LocalDateTime exp = claims.getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return RefreshToken.builder()
                .id(claims.get("jti", String.class))
                .user(user)
                .deviceType(DeviceType.WEB)
                .expiresAt(exp)
                .build();
    }

    private void populateResponse(String access, String refresh, HttpServletResponse response) {
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        response.addCookie(cookieUtil.createCookie("refresh", refresh));
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_OK);
    }

}
