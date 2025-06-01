package com.flyby.ramble.auth.service;

import com.flyby.ramble.auth.dto.JwtTokenRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.expiration-ms.refresh}")
    private long refreshExpiration;

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    // TODO: DeviceType 구분 로직 필요, 현재는 WEB으로 고정
    // TODO: jwt 파싱 안 하고 생성한 정보를 기반으로 db에 저장하게

    public Tokens generateTokens(User user) {
        JwtTokenRequest accRequest = JwtTokenRequest.of(user, DeviceType.WEB);
        JwtTokenRequest refRequest = JwtTokenRequest.of(user, DeviceType.WEB);

        String accToken = jwtUtil.generateAccToken(accRequest);
        String refToken = jwtUtil.generateRefToken(refRequest);

        refreshTokenRepository.save(createRefreshToken(user, refRequest));

        return new Tokens(accToken, refToken);
    }

    public Tokens reissueTokens(String refToken) {
        Claims refClaims = parseToken(refToken);
        String jti       = refClaims.get("jti", String.class);
        UUID   userId    = UUID.fromString(refClaims.getSubject());
        DeviceType type  = DeviceType.valueOf(refClaims.get("deviceType", String.class));

        RefreshToken refreshToken = refreshTokenRepository.findByIdAndRevokedFalse(UUID.fromString(jti))
                .orElseThrow(() -> {
                    log.warn("Invalid refresh token used: {}, userId: {}, deviceType: {}", jti, userId, type);
                    refreshTokenRepository.revokeAllByUserIdAndDeviceType(userId, type);
                    return new BaseException(ErrorCode.ACCESS_DENIED);
                });

        refreshTokenRepository.save(refreshToken.revoke());
        return generateTokens(refreshToken.getUser());
    }

    public void revokeAllRefreshTokenByUserAndDevice(String userId, DeviceType deviceType) {
        refreshTokenRepository.revokeAllByUserIdAndDeviceType(UUID.fromString(userId), deviceType);
    }

    public void revokeAllRefreshTokenByUser(String userId) {
        refreshTokenRepository.revokeAllByUserExternalId(UUID.fromString(userId));
    }


    private Claims parseToken(String token) {
        try {
            return jwtUtil.parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private RefreshToken createRefreshToken(User user, JwtTokenRequest request) {
        LocalDateTime exp = Instant.now()
                .plusMillis(refreshExpiration)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return RefreshToken.builder()
                .id(request.jti())
                .user(user)
                .deviceType(request.deviceType())
                .expiresAt(exp)
                .build();
    }


    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpExpiredRefreshTokens() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        refreshTokenRepository.deleteAllByExpiresAtBeforeOrRevokedTrue(now);
    }

}
