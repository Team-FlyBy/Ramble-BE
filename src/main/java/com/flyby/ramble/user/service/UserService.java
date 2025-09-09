package com.flyby.ramble.user.service;

import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.oauth.dto.OAuthRegisterDTO;
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

import java.util.UUID;

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
    @Cacheable(value = "user", key = "#userExternalId", unless = "#result == null")
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

    @CachePut(value = "user", key = "#result.externalId", unless = "#result == null")
    public UserInfoDTO registerOrLogin(OAuthRegisterDTO oAuthRegisterDTO) {
        try {
            return userRepository.findByProviderAndProviderId(oAuthRegisterDTO.provider(), oAuthRegisterDTO.providerId())
                    .map(UserInfoDTO::from)
                    .orElseGet(() -> UserInfoDTO.from(userRepository.save(
                            User.builder()
                                    .email(oAuthRegisterDTO.email())
                                    .username(oAuthRegisterDTO.username())
                                    .provider(oAuthRegisterDTO.provider())
                                    .providerId(oAuthRegisterDTO.providerId())
                                    .gender(oAuthRegisterDTO.gender())
                                    .birthDate(oAuthRegisterDTO.birthDate())
                                    .build()))
                    );
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("예상치 못한 오류가 발생했습니다", e);
        }
    }

    @CacheEvict(value = "user", key = "#userExternalId")
    public void withdraw(String userExternalId) {
        User user = userRepository.findByExternalId(UUID.fromString(userExternalId))
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        userRepository.save(user.anonymize());
    }

}
