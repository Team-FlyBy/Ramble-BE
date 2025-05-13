package com.flyby.ramble.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Role {
    USER,
    MANAGER,
    ADMIN
}
