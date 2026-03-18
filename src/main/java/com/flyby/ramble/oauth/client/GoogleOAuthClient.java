package com.flyby.ramble.oauth.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.oauth.constants.OAuthConstants;
import com.flyby.ramble.oauth.dto.OAuthPersonInfo;
import com.flyby.ramble.user.model.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Google People API를 통해 사용자의 성별과 생년월일 정보를 가져옴
     * scope에 따라 사용 가능한 정보만 반환
     */
    public OAuthPersonInfo getPersonInfo(OAuth2AccessToken accessToken) {
        Set<String> scopes = accessToken.getScopes();
        Optional<String> personFields = buildPersonFields(scopes);

        if (personFields.isEmpty()) {
            return new OAuthPersonInfo(Gender.UNKNOWN, null);
        }

        String url = UriComponentsBuilder.fromUriString(OAuthConstants.GOOGLE_PEOPLE_API_URL)
                .queryParam("personFields", personFields.get())
                .queryParam("sources", "READ_SOURCE_TYPE_PROFILE")
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.getTokenValue());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parsePersonInfo(response.getBody(), scopes);
            }
        } catch (Exception e) {
            log.error("Failed to fetch person info from Google People API", e);
        }

        return new OAuthPersonInfo(Gender.UNKNOWN, null);
    }

    public void revokeToken(String token) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            restTemplate.postForEntity(OAuthConstants.GOOGLE_REVOKE_URL, request, String.class);
        } catch (Exception e) {
            log.error("Failed to revoke Google OAuth token", e);
        }
    }

    private boolean hasGenderScope(Set<String> scopes)   {
        return scopes.contains(OAuthConstants.GOOGLE_SCOPE_GENDER);
    }

    private boolean hasBirthdayScope(Set<String> scopes) {
        return scopes.contains(OAuthConstants.GOOGLE_SCOPE_BIRTHDAY);
    }

    private Optional<String> buildPersonFields(Set<String> scopes) {
        List<String> fields = new ArrayList<>();
        if (hasGenderScope(scopes))   fields.add("genders");
        if (hasBirthdayScope(scopes)) fields.add("birthdays");

        return fields.isEmpty() ? Optional.empty() : Optional.of(String.join(",", fields));
    }

    private OAuthPersonInfo parsePersonInfo(String responseBody, Set<String> scopes) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String gender = extractGender(root, scopes);
            LocalDate birthDate = extractBirthDate(root, scopes);

            return new OAuthPersonInfo(Gender.from(gender), birthDate);
        } catch (Exception e) {
            log.error("Failed to parse person info response", e);
            return new OAuthPersonInfo(Gender.UNKNOWN, null);
        }
    }

    private String extractGender(JsonNode root, Set<String> scopes) {
        if (!hasGenderScope(scopes)) {
            return null;
        }

        return root.path("genders").path(0).path("value").asText(null);
    }

    private LocalDate extractBirthDate(JsonNode root, Set<String> scopes) {
        if (!hasBirthdayScope(scopes)) {
            return null;
        }

        JsonNode birthdays = root.path("birthdays").path(0).path("date");
        if (birthdays.isMissingNode() || !birthdays.has("year")) {
            return null;
        }

        int year  = birthdays.path("year").asInt(1);
        int month = birthdays.path("month").asInt(1);
        int day   = birthdays.path("day").asInt(1);

        return LocalDate.of(year, month, day);
    }

}
