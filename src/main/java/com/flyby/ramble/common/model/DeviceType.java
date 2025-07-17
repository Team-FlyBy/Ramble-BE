package com.flyby.ramble.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum DeviceType {
    ANDROID,
    IOS,
    WEB;

    public static DeviceType from(String deviceType) {
        try {
            return DeviceType.valueOf(deviceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid device type: " + deviceType);
        }
    }
}
