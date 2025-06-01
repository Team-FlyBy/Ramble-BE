package com.flyby.ramble.user.repository;

import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Status;
import com.flyby.ramble.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    Optional<User> findByExternalId(UUID externalId);

    @Modifying
    @Query("UPDATE User u "
            + "SET u.email = :newEmail, "
            + "    u.status = :status, "
            + "    u.username = :newUsername, "
            + "    u.providerId = :newProviderId "
            + "WHERE u.id = :userId")
    void anonymizeFields(
            @Param("userId") Long userId,
            @Param("status") Status status,
            @Param("newEmail") String newEmail,
            @Param("newUsername") String newUsername,
            @Param("newProviderId") String newProviderId
    );
}
