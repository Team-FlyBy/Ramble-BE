package com.flyby.ramble.matching.controller;

import com.flyby.ramble.common.service.GeoIpService;
import com.flyby.ramble.matching.dto.MatchRequestDTO;
import com.flyby.ramble.matching.service.MatchingService;
import com.flyby.ramble.signaling.dto.SignalMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MatchingController {

    private final GeoIpService geoIpService;
    private final MatchingService matchingService;

    @MessageMapping("/match/request")
    public void handleMatchRequest(@Payload MatchRequestDTO request,
                                   SimpMessageHeaderAccessor headerAccessor,
                                   Principal principal) {
        String userId = getUserId(principal);
        String region = geoIpService.getCountryCode(getUserIp(headerAccessor));

        matchingService.findMatchOrAddToQueue(userId, region, request);
    }

    @MessageMapping("/match/signaling")
    public void handleSignalingMessage(@Payload SignalMessage message, Principal principal) {
        String userId = getUserId(principal);

        matchingService.forwardSignalingMessage(userId, message);
    }

    @MessageMapping("/match/cancel")
    public void handleCancelMatch(Principal principal) {
        String userId = getUserId(principal);

        matchingService.cleanupUser(userId);
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

        return sessionAttrs.get("ip").toString();
    }

}
