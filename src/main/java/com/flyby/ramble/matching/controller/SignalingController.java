package com.flyby.ramble.matching.controller;

import com.flyby.ramble.common.service.GeoIpService;
import com.flyby.ramble.matching.dto.MatchRequestDTO;
import com.flyby.ramble.matching.dto.SignalMessageDTO;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class SignalingController {
    private final GeoIpService geoIpService;
    private final MatchingService matchingService;

    @Deprecated(since = "2026-02-08", forRemoval = true)
    @MessageMapping("/match/request")
    public void handleMatchRequest(@Payload MatchRequestDTO request,
                                   SimpMessageHeaderAccessor headerAccessor,
                                   Principal principal) {
        String userId = getUserId(principal);
        String region = geoIpService.getCountryCode(getUserIp(headerAccessor));

        matchingService.requestMatch(userId, Region.from(region), request);
    }

    @MessageMapping("/match/signaling")
    public void handleSignalingMessage(@Payload SignalMessageDTO message, Principal principal) {
        String userId = getUserId(principal);
        message.setSenderId(userId);

        matchingService.relaySignal(userId, message);
    }

    @Deprecated(since = "2026-02-08", forRemoval = true)
    @MessageMapping("/match/cancel")
    public void handleCancelMatch(Principal principal) {
        String userId = getUserId(principal);

        matchingService.disconnectUser(userId);
    }

    private String getUserId(Principal principal) {
        return Optional.ofNullable(principal)
                .map(Principal::getName)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자 ID가 유효하지 않습니다."));
    }

    private String getUserIp(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            throw new IllegalArgumentException("헤더 액세서가 null 입니다.");
        }

        var sessionAttrs = headerAccessor.getSessionAttributes();

        if (sessionAttrs == null) {
            throw new IllegalArgumentException("세션 속성이 존재하지 않습니다.");
        }

        Object ip = sessionAttrs.get("ip");

        if (ip == null) {
            throw new IllegalArgumentException("세션에 IP 정보가 없습니다.");
        }

        return ip.toString();
    }


}
