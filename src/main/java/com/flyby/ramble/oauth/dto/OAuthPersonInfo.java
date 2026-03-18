package com.flyby.ramble.oauth.dto;

import com.flyby.ramble.user.model.Gender;

import java.time.LocalDate;

public record OAuthPersonInfo(
        Gender gender,
        LocalDate birthDate
) {
}