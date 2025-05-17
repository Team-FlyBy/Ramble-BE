package com.flyby.ramble.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider", columnNames = "provider"),
                @UniqueConstraint(name = "uk_provider_id", columnNames = "provider_id")
        }
)
public class User extends BaseEntity {

    // TODO: validation 필요 (email 등)
    // TODO: age, gender 추가 필요

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "external_id", columnDefinition = "BINARY(16)",
            nullable = false, updatable = false, unique = true)
    private UUID externalId;

    @Setter
    @Column(name = "user_name", nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @PrePersist
    public void assignExternalId() {
        if (externalId == null) {
            externalId = UUID.randomUUID();
        }
    }

    @Builder
    public User(String username, String email, OAuthProvider provider, String providerId, Role role) {
        this.username = username;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

}
