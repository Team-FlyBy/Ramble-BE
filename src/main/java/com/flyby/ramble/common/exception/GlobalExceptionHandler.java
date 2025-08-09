package com.flyby.ramble.common.exception;

import com.flyby.ramble.common.dto.ResponseDTO;
import com.flyby.ramble.common.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDTO<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Invalid request body", e);
        return ResponseUtil.error(ErrorCode.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ResponseDTO<Object>> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.warn("Missing required header: {}", e.getHeaderName());
        return ResponseUtil.error(ErrorCode.MISSING_REQUIRED_HEADER);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ResponseDTO<Object>> handleMissingRequestCookieException(MissingRequestCookieException e) {
        log.warn("Missing required cookie: {}", e.getCookieName());
        return ResponseUtil.error(ErrorCode.MISSING_REFRESH_TOKEN);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ResponseDTO<Object>> handleBaseException(BaseException e) {
        log.warn(e.getMessage(), e);
        return ResponseUtil.error(e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Object>> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseUtil.error(ErrorCode.UNEXPECTED_SERVER_ERROR);
    }

}
