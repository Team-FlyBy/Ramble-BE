package com.flyby.ramble.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flyby.ramble.user.model.Gender;
import com.flyby.ramble.user.model.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfoDTO {
    @JsonIgnore
    private long id;
    private UUID externalId;
    private String username;
    private String email;
    private Gender gender;
    private LocalDate birthDate;

    @Builder
    public UserInfoDTO(Long id, UUID externalId, String username, String email, Gender gender, LocalDate birthDate) {
        if (id == null | externalId == null || username == null || email == null){
            throw new IllegalArgumentException("id, externalId, username, email은 null일 수 없습니다.");
        }

        this.id = id;
        this.externalId = externalId;
        this.username = username;
        this.email = email;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public static UserInfoDTO from(User user) {
        return UserInfoDTO.builder()
                .id(user.getId())
                .externalId(user.getExternalId())
                .username(user.getUsername())
                .email(user.getEmail())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .build();
    }
}
