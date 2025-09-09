package com.flyby.ramble.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Gender;
import com.flyby.ramble.user.model.Role;
import com.flyby.ramble.user.model.User;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserInfoDTO {
    @JsonIgnore
    private Long id;
    private String externalId;
    private String username;
    private String email;
    private OAuthProvider provider;

    @JsonIgnore
    private String providerId;

    @JsonIgnore
    private Role role;

    @JsonIgnore
    private Gender gender;

    @JsonIgnore
    private LocalDate birthDate;

    public static UserInfoDTO from(User user) {
        return UserInfoDTO.builder()
                .id(user.getId())
                .externalId(user.getExternalId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .role(user.getRole())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .build();
    }
}
