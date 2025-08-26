package com.flyby.ramble.oauth.dto;

import java.time.LocalDate;

public record GooglePersonInfo(
        String gender,
        LocalDate birthDate
) {
}