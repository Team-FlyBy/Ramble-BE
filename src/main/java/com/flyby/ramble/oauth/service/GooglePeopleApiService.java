package com.flyby.ramble.oauth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyby.ramble.oauth.dto.GooglePersonInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePeopleApiService {

    private static final String PEOPLE_API_URL = "https://people.googleapis.com/v1/people/me";
    private static final String SCOPE_BIRTHDAY = "https://www.googleapis.com/auth/user.birthday.read";
    private static final String SCOPE_GENDER   = "https://www.googleapis.com/auth/user.gender.read";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Google People API를 통해 사용자의 성별과 생년월일 정보를 가져옴
     * scope에 따라 사용 가능한 정보만 반환
     */
    public GooglePersonInfo getPersonInfo(OAuth2AccessToken accessToken) {
        Set<String> scopes = accessToken.getScopes();
        Optional<String> personFields = buildPersonFields(scopes);

        if (personFields.isEmpty()) {
            return new GooglePersonInfo(null, null);
        }

        String url = UriComponentsBuilder.fromUriString(PEOPLE_API_URL)
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

        return new GooglePersonInfo(null, null);
    }

    private boolean hasGenderScope(Set<String> scopes)   {
        return scopes.contains(SCOPE_GENDER);
    }

    private boolean hasBirthdayScope(Set<String> scopes) {
        return scopes.contains(SCOPE_BIRTHDAY);
    }

    private Optional<String> buildPersonFields(Set<String> scopes) {
        List<String> fields = new ArrayList<>();
        if (hasGenderScope(scopes))   fields.add("genders");
        if (hasBirthdayScope(scopes)) fields.add("birthdays");

        return fields.isEmpty() ? Optional.empty() : Optional.of(String.join(",", fields));
    }

    private GooglePersonInfo parsePersonInfo(String responseBody, Set<String> scopes) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String gender = extractGender(root, scopes);
            LocalDate birthDate = extractBirthDate(root, scopes);

            return new GooglePersonInfo(gender, birthDate);
        } catch (Exception e) {
            log.error("Failed to parse person info response", e);
            return new GooglePersonInfo(null, null);
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
