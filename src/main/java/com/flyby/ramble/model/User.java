package com.flyby.ramble.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_provider_provider_id",
                columnNames = {"provider", "provider_id"}
        ))
public class User extends BaseEntity {

    // TODO: validation 필요 (email 등)
    // TODO: age, gender 추가 필요
    // TODO: 차단, 탈퇴, 삭제 등 비활성화 정보 필요

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
    @Column(nullable = false, updatable = false)
    private OAuthProvider provider;

    @Column(nullable = false, updatable = false)
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
        if (username.isEmpty() || email.isEmpty() || provider == null || providerId.isEmpty()) {
            throw new IllegalArgumentException("필수 필드는 null이 될 수 없습니다");
        }

        this.username = username;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

}
