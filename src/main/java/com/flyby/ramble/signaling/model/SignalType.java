package com.flyby.ramble.signaling.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum SignalType {
    OFFER,
    ANSWER,
    CANDIDATE
}
