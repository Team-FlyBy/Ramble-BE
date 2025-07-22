package com.flyby.ramble.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 400 BAD_REQUEST
    INVALID_REQUEST_BODY (400, HttpStatus.BAD_REQUEST, "Invalid Request Body"),
    MISSING_REQUIRED_HEADER(400, HttpStatus.BAD_REQUEST, "Missing Required Header"),

    INVALID_DEVICE_TYPE(40001, HttpStatus.BAD_REQUEST, "Invalid Device Type"),

    // 401 UNAUTHORIZED
    MISSING_ACCESS_TOKEN (40001, HttpStatus.UNAUTHORIZED, "Missing Access Token"),
    INVALID_ACCESS_TOKEN (40002, HttpStatus.UNAUTHORIZED, "Invalid Access Token"),
    EXPIRED_ACCESS_TOKEN (40003, HttpStatus.UNAUTHORIZED, "Expired Access Token"),
    BLOCKED_ACCESS_TOKEN (40004, HttpStatus.UNAUTHORIZED, "Blocked Access Token"),
    MISSING_REFRESH_TOKEN(40011, HttpStatus.UNAUTHORIZED, "Missing Refresh Token"),
    INVALID_REFRESH_TOKEN(40012, HttpStatus.UNAUTHORIZED, "Invalid Refresh Token"),
    EXPIRED_REFRESH_TOKEN(40013, HttpStatus.UNAUTHORIZED, "Expired Refresh Token"),
    ACCESS_DENIED(40021, HttpStatus.UNAUTHORIZED, "Access Denied"),

    // 403 FORBIDDEN
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "Forbidden"),

    // 404 NOT_FOUND
    USER_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "User Not Found"),

    // 409 CONFLICT

    // 500 INTERNAL_SERVER_ERROR
    UNEXPECTED_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    // TODO: code 정의 필요. (현재 임의의 값)
}
