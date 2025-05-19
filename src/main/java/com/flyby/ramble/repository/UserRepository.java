package com.flyby.ramble.repository;

import com.flyby.ramble.model.OAuthProvider;
import com.flyby.ramble.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

}
