package com.flyby.ramble.service;

import com.flyby.ramble.model.OAuthProvider;
import com.flyby.ramble.model.Role;
import com.flyby.ramble.model.User;
import com.flyby.ramble.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User registerOrLogin(String email, String username, OAuthProvider provider, String providerId) {
        Optional<User> optUser = userRepository.findByProviderAndProviderId(provider, providerId);

        if (optUser.isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .username(username)
                    .provider(provider)
                    .providerId(providerId)
                    .role(Role.USER)
                    .build();

            return userRepository.save(user);
        }

        return optUser.get();
    }


}
