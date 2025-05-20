package com.flyby.ramble.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum DeviceType {
    ANDROID,
    IOS,
    WEB
}
