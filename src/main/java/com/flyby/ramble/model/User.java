package com.flyby.ramble.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    // TODO: validation 필요 (email 등)
    // TODO: age, gender 추가 필요

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Setter
    private String userName;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public User(String userName, String email, OAuthProvider provider, Role role) {
        this.userName = userName;
        this.email = email;
        this.provider = provider;
        this.role = role;
    }

}
