package com.flyby.ramble.user.repository;

import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

}
