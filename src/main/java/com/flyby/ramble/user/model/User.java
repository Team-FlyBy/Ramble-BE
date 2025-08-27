package com.flyby.ramble.user.model;

import com.flyby.ramble.auth.model.RefreshToken;
import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.common.model.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @Builder
    public User(@NonNull String username, @NonNull String email, @NonNull OAuthProvider provider, @NonNull String providerId, @NonNull Gender gender, LocalDate birthDate) {
        if (username.isBlank() || email.isBlank() || providerId.isBlank()) {
            throw new IllegalArgumentException("필수 필드는 빈 값이 될 수 없습니다");
        }

        this.username = username;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.gender = gender;
        this.birthDate = birthDate;

        this.externalId = UUID.randomUUID();
        this.role = Role.ROLE_USER;
        this.status = Status.ACTIVE;
    }

    public User anonymize() {
        this.username = "anonymous_user_" + this.externalId;
        this.email = "anonymous_email_" + this.externalId + "@example.com";
        this.providerId = "anonymous_provider_id_" + this.externalId;
        this.gender = Gender.UNKNOWN;
        this.birthDate = null;
        this.status = Status.INACTIVE;

        return this;
    }

}
