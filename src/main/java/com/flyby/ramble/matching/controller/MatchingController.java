package com.flyby.ramble.matching.controller;

import com.flyby.ramble.common.annotation.SwaggerApi;
import com.flyby.ramble.common.dto.ResponseDTO;
import com.flyby.ramble.common.service.GeoIpService;
import com.flyby.ramble.common.util.ResponseUtil;
import com.flyby.ramble.matching.dto.MatchRequestDTO;
import com.flyby.ramble.matching.dto.MatchResultDTO;
import com.flyby.ramble.matching.model.Region;
import com.flyby.ramble.matching.service.MatchingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/match")
public class MatchingController {
    private final GeoIpService geoIpService;
    private final MatchingService matchingService;

    @SwaggerApi(
            summary = "매칭 요청",
            description = "매칭 요청 API"
    )
    @PostMapping("/request")
    public ResponseEntity<ResponseDTO<MatchResultDTO>> handleMatchRequest(HttpServletRequest servletRequest,
                                                                            @RequestBody MatchRequestDTO request,
                                                                            @AuthenticationPrincipal UserDetails user) {
        String userId = user.getUsername();
        String region = geoIpService.getCountryCode(servletRequest.getRemoteAddr());

        MatchResultDTO result = matchingService.requestMatch(userId, Region.from(region), request);

        return ResponseUtil.success(result);
    }

    @SwaggerApi(
            summary = "매칭 취소",
            description = "매칭 취소 요청 API",
            responseCode = "204",
            responseDescription = "No Content"
    )
    @PostMapping("/cancel")
    public ResponseEntity<Void> handleCancelMatch(@AuthenticationPrincipal UserDetails user) {
        String userId = user.getUsername();

        matchingService.disconnectUser(userId);

        return ResponseEntity.noContent().build();
    }

}
