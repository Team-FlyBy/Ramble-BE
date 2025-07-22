package com.flyby.ramble.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum DeviceType {
    ANDROID,
    IOS,
    WEB;

    public static DeviceType from(String deviceType) {
        try {
            return DeviceType.valueOf(deviceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_DEVICE_TYPE);
        }
    }
}
