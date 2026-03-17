package com.flyby.ramble.user.service;

import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.oauth.dto.OAuthPersonInfo;
import com.flyby.ramble.oauth.dto.OAuthRevokeInfo;
import com.flyby.ramble.oauth.dto.OidcTokenInfo;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getUserProxyById(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "user", key = "#userExternalId", unless = "#result == null")
    public UserInfoDTO getUserByExternalId(String userExternalId) {
        UUID externalId;
        try {
            externalId = UUID.fromString(userExternalId);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findByExternalId(externalId)
                .map(UserInfoDTO::from)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    @CachePut(cacheNames = "user", key = "#result.externalId", unless = "#result == null")
    public UserInfoDTO findOrCreateUser(OidcTokenInfo tokenInfo, Supplier<OAuthPersonInfo> personInfoSupplier, String oauthRefreshToken) {
        Optional<User> optionalUser = userRepository.findByProviderAndProviderId(tokenInfo.provider(), tokenInfo.providerId());

        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();

            if (oauthRefreshToken != null) {
                log.info("refresh token: {}", oauthRefreshToken);
                existingUser.updateOauthRefreshToken(oauthRefreshToken);
            }

            return UserInfoDTO.from(existingUser);
        }

        OAuthPersonInfo personInfo = personInfoSupplier.get();
        return registerUser(tokenInfo, personInfo, oauthRefreshToken);
    }

    @CacheEvict(cacheNames = "user", key = "#userExternalId")
    public OAuthRevokeInfo withdraw(String userExternalId) {
        User user = userRepository.findByExternalId(UUID.fromString(userExternalId))
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        OAuthRevokeInfo revokeInfo = new OAuthRevokeInfo(user.getProvider(), user.getOauthRefreshToken());
        userRepository.save(user.anonymize());

        return revokeInfo;
    }

    private UserInfoDTO registerUser(OidcTokenInfo tokenInfo, OAuthPersonInfo personInfo, String oauthRefreshToken) {
        try {
            User newUser = User.builder()
                    .email(tokenInfo.email())
                    .username(tokenInfo.username())
                    .provider(tokenInfo.provider())
                    .providerId(tokenInfo.providerId())
                    .gender(personInfo.gender())
                    .birthDate(personInfo.birthDate())
                    .build();

            if (oauthRefreshToken != null) {
                newUser.updateOauthRefreshToken(oauthRefreshToken);
            }

            return UserInfoDTO.from(userRepository.save(newUser));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 유저가 이미 생성된 경우 재조회
            log.warn("동시 유저 생성 감지, 재조회 시도: provider={}, providerId={}", tokenInfo.provider(), tokenInfo.providerId());
            return userRepository.findByProviderAndProviderId(tokenInfo.provider(), tokenInfo.providerId())
                    .map(UserInfoDTO::from)
                    .orElseThrow(() -> new BaseException(ErrorCode.UNEXPECTED_SERVER_ERROR));
        }
    }

}
