package com.flyby.ramble.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Gender {
    FEMALE,
    MALE,
    UNKNOWN;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Gender from(String gender) {
        if (gender == null) {
            return UNKNOWN;
        }

        return switch (gender.trim().toLowerCase()) {
            case "female","f" -> FEMALE;
            case "male","m"   -> MALE;
            default           -> UNKNOWN;
        };
    }

}
