package com.flyby.ramble.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 400 BAD_REQUEST
    INVALID_REQUEST_BODY (400, HttpStatus.BAD_REQUEST, "Invalid Request Body"),

    // 401 UNAUTHORIZED
    MISSING_ACCESS_TOKEN (40001, HttpStatus.UNAUTHORIZED, "Missing Access Token"),
    MISSING_REFRESH_TOKEN(40002, HttpStatus.UNAUTHORIZED, "Missing Refresh Token"),
    INVALID_ACCESS_TOKEN (40003, HttpStatus.UNAUTHORIZED, "Invalid Access Token"),
    EXPIRED_ACCESS_TOKEN (40004, HttpStatus.UNAUTHORIZED, "Expired Access Token"),
    INVALID_REFRESH_TOKEN(40005, HttpStatus.UNAUTHORIZED, "Invalid Refresh Token"),
    EXPIRED_REFRESH_TOKEN(40006, HttpStatus.UNAUTHORIZED, "Expired Refresh Token"),
    ACCESS_DENIED(400067, HttpStatus.UNAUTHORIZED, "Access Denied"),

    // 403 FORBIDDEN
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "Forbidden"),

    // 404 NOT_FOUND

    // 409 CONFLICT

    // 500 INTERNAL_SERVER_ERROR
    UNEXPECTED_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    // TODO: code 정의 필요. (현재 임의의 값)
}
