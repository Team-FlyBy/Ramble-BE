package com.flyby.ramble.common.util;

import com.flyby.ramble.common.exception.ErrorCode;
import com.flyby.ramble.common.model.ResponseDTO;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

@UtilityClass
public class ResponseUtil {

    public <T> ResponseEntity<ResponseDTO<T>> success(HttpStatus httpStatus, String message, T body) {
        return ResponseEntity
                .status(httpStatus)
                .body(new ResponseDTO<>(httpStatus.value(), message, body));
    }

    public <T> ResponseEntity<ResponseDTO<T>> success(String message, T body) {
        return success(HttpStatus.OK, message, body);
    }

    public <T> ResponseEntity<ResponseDTO<T>> success(T body) {
        return success("성공적으로 처리 완료", body);
    }

    public ResponseEntity<ResponseDTO<Object>> error(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ResponseDTO<>(errorCode.getCode(), errorCode.getMessage(), Collections.emptyMap()));
    }

}
