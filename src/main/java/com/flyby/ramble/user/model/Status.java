package com.flyby.ramble.user.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Status {
    ACTIVE,   // 활성 상태
    INACTIVE, // 비활성 상태
    BLOCKED   // 정지 상태
}
