package com.flyby.ramble.user.model;

import com.flyby.ramble.auth.model.RefreshToken;
import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.common.model.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_provider_provider_id",
                columnNames = {"provider", "provider_id"}
        ),
        indexes = { @Index(name = "idx_user_external_id", columnList = "external_id") }
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

    @Column(name = "user_name", nullable = false)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, updatable = false)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, updatable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @Builder
    public User(String username, String email, OAuthProvider provider, String providerId) {
        if (username == null || email == null || provider == null || providerId == null) {
            throw new IllegalArgumentException("필수 필드는 null이 될 수 없습니다");
        }

        if (username.isEmpty() || email.isEmpty() || providerId.isEmpty()) {
            throw new IllegalArgumentException("필수 필드는 빈 값이 될 수 없습니다");
        }

        this.username = username;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;

        this.externalId = UUID.randomUUID();
        this.role = Role.ROLE_USER;
        this.status = Status.ACTIVE;
    }

}
