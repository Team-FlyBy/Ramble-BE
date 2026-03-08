package com.flyby.ramble.matching.dto;

import com.flyby.ramble.session.dto.SessionData;

import java.util.List;
import java.util.Map;

public record MatchRoundResult(
    List<SessionData> matched,
    Map<String, MatchingProfile> remaining
) {}
