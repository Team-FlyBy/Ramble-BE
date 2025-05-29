package com.flyby.ramble.user.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Role {
    ROLE_USER,
    ROLE_MANAGER,
    ROLE_ADMIN
}
