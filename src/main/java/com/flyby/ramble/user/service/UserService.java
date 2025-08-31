package com.flyby.ramble.user.service;

import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.oauth.dto.OAuthRegisterDTO;
import com.flyby.ramble.user.dto.UserInfoDTO;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#userExternalId", unless = "#result == null")
    public UserInfoDTO getUserByExternalId(String userExternalId) {
        return userRepository.findByExternalId(UUID.fromString(userExternalId))
                .map(UserInfoDTO::from)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    public User getUserProxyById(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    // TODO: 동일한 유저의 중복 생성 가능성 판단 후 수정
    // TODO: 커스텀 예외 처리 추후 추가
    public User registerOrLogin(OAuthRegisterDTO oAuthRegisterDTO) {
        try {
            return userRepository.findByProviderAndProviderId(oAuthRegisterDTO.provider(), oAuthRegisterDTO.providerId())
                    .orElseGet(() -> userRepository.save(User.builder()
                            .email(oAuthRegisterDTO.email())
                            .username(oAuthRegisterDTO.username())
                            .provider(oAuthRegisterDTO.provider())
                            .providerId(oAuthRegisterDTO.providerId())
                            .gender(oAuthRegisterDTO.gender())
                            .birthDate(oAuthRegisterDTO.birthDate())
                            .build()));
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByProviderAndProviderId(oAuthRegisterDTO.provider(), oAuthRegisterDTO.providerId())
                    .orElseThrow(() -> new IllegalArgumentException("예상치 못한 오류가 발생했습니다", e));
        }
    }

    public void withdraw(String userExternalId) {
        User user = userRepository.findByExternalId(UUID.fromString(userExternalId))
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        userRepository.save(user.anonymize());
    }

}
