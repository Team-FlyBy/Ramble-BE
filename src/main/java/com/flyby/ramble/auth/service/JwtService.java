package com.flyby.ramble.auth.service;

import com.flyby.ramble.auth.dto.Tokens;
import com.flyby.ramble.auth.model.RefreshToken;
import com.flyby.ramble.auth.repository.RefreshTokenRepository;
import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    // TODO: DeviceType 구분 로직 필요, 현재는 WEB으로 고정

    public Tokens generateTokens(User user) {
        String accToken  = jwtUtil.generateAccToken(user.getExternalId().toString(), user.getRole(), DeviceType.WEB, user.getProvider(), user.getProviderId());
        String refToken  = jwtUtil.generateRefToken(user.getExternalId().toString(), user.getRole(), DeviceType.WEB, user.getProvider(), user.getProviderId());
        Claims refClaims = parseToken(refToken);

        refreshTokenRepository.save(createRefreshToken(user, refClaims));

        return new Tokens(accToken, refToken);
    }

    public Tokens reissueTokens(String refToken) {
        Claims refClaims = parseToken(refToken);
        String jti       = refClaims.get("jti", String.class);
        UUID   userId    = UUID.fromString(refClaims.getSubject());
        DeviceType type  = DeviceType.valueOf(refClaims.get("deviceType", String.class));

        RefreshToken refreshToken = refreshTokenRepository.findByIdAndRevokedFalse(jti)
                .orElseThrow(() -> {
                    log.warn("Invalid refresh token used: {}, userId: {}, deviceType: {}", jti, userId, type);
                    refreshTokenRepository.revokeAllByUserIdAndDeviceType(userId, type);
                    return new BaseException(ErrorCode.ACCESS_DENIED);
                });

        refreshTokenRepository.save(refreshToken.revoke());
        return generateTokens(refreshToken.getUser());
    }

    public void revokeAllRefreshToken(String userId, DeviceType deviceType) {
        refreshTokenRepository.revokeAllByUserIdAndDeviceType(UUID.fromString(userId), deviceType);
    }

    public void revokeAllRefreshToken(String userId) {
        refreshTokenRepository.revokeAllByUserExternalId(UUID.fromString(userId));
    }

    /*  */

    private Claims parseToken(String token) {
        try {
            return jwtUtil.parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private RefreshToken createRefreshToken(User user, Claims claims) {
        String deviceType = claims.get("deviceType", String.class);
        LocalDateTime exp = claims.getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return RefreshToken.builder()
                .id(claims.get("jti", String.class))
                .user(user)
                .deviceType(DeviceType.valueOf(deviceType))
                .expiresAt(exp)
                .build();
    }

    /*  */

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpExpiredRefreshTokens() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        refreshTokenRepository.deleteAllByExpiresAtBeforeOrRevokedTrue(now);
    }

}
