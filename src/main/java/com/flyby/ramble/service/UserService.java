package com.flyby.ramble.service;

import com.flyby.ramble.model.OAuthProvider;
import com.flyby.ramble.model.Role;
import com.flyby.ramble.model.User;
import com.flyby.ramble.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // TODO: 동일한 유저의 중복 생성 가능성 판단 후 수정
    // TODO: 커스텀 예외 처리 추후 추가
    public User registerOrLogin(String email, String username, OAuthProvider provider, String providerId) {
        try {
            return userRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseGet(() -> userRepository.save(User.builder()
                            .email(email)
                            .username(username)
                            .provider(provider)
                            .providerId(providerId)
                            .role(Role.USER)
                            .build()));
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> new IllegalArgumentException("예상치 못한 오류가 발생했습니다", e));
        }
    }

}
