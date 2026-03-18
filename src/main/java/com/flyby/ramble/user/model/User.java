package com.flyby.ramble.user.model;

import com.flyby.ramble.auth.util.EncryptConverter;
import com.flyby.ramble.common.model.BaseEntity;
import com.flyby.ramble.common.model.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    @Column(name = "user_name") // , nullable = false) apple oauth 기준 name, email을 못 얻어올 수 있음
    private String username;

    @Column(name = "email") //, nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, updatable = false)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "oauth_refresh_token", length = 1024)
    @Convert(converter = EncryptConverter.class)
    private String oauthRefreshToken;

    @Builder
    public User(String username, String email, @NonNull OAuthProvider provider, @NonNull String providerId, @NonNull Gender gender, LocalDate birthDate) {
        if (providerId.isBlank()) {
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

    public User updateOauthRefreshToken(String token) {
        this.oauthRefreshToken = token;

        return this;
    }

    public User anonymize() {
        this.username = "anonymous_user_" + this.externalId;
        this.email = "anonymous_email_" + this.externalId + "@example.com";
        this.providerId = "anonymous_provider_id_" + this.externalId;
        this.oauthRefreshToken = null;
        this.gender = Gender.UNKNOWN;
        this.birthDate = null;
        this.status = Status.INACTIVE;

        return this;
    }

}
