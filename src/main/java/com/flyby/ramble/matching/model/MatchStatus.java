package com.flyby.ramble.matching.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum MatchStatus {
    SUCCESS,
    WAITING,
    FAILED,
    LEAVE,
}
