package com.flyby.ramble.common.interceptor;

import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.exception.BaseException;
import com.flyby.ramble.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authToken = resolveBearerToken(
                accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)
        );

        Authentication auth = parseAuthentication(authToken);
        SecurityContextHolder.getContext().setAuthentication(auth);
        accessor.setUser(auth);

        return message;
    }

    private Authentication parseAuthentication(String authToken) {
        try {
            return jwtUtil.parseAuthentication(authToken);
        } catch (ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    private String resolveBearerToken(String header) {
        if (!StringUtils.hasText(header)) {
            throw new BaseException(ErrorCode.MISSING_ACCESS_TOKEN);
        }

        if (!header.startsWith("Bearer ")) {
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        return header.substring(7).trim();
    }

}
