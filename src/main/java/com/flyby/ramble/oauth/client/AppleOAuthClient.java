package com.flyby.ramble.oauth.client;

import com.flyby.ramble.oauth.constants.OAuthConstants;
import com.flyby.ramble.oauth.dto.AppleTokenResponse;
import com.flyby.ramble.oauth.properties.OAuthProperties;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleOAuthClient {
    private final OAuthProperties oauthProperties;
    private final RestTemplate restTemplate;

    /**
     * Apple Sign In에 필요한 ES256 서명된 JWT client_secret을 생성한다.
     * .p8 키 확보 후 실제 서명 검증 필요.
     */
    public String createAppleClientSecret(String clientId) {
        if (oauthProperties.getPrivateKey().isEmpty()) {
            throw new IllegalStateException("Apple .p8 private key가 설정되지 않았습니다");
        }

        Instant now = Instant.now();

        return Jwts.builder()
                .header().keyId(oauthProperties.getKeyId()).and()
                .issuer(oauthProperties.getTeamId())
                .audience().add("https://appleid.apple.com").and()
                .subject(clientId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300))) // 5분
                .signWith(loadPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    /**
     * Apple authorization code를 사용해 토큰 엔드포인트에서 토큰을 교환한다.
     */
    public AppleTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        String clientSecret = createAppleClientSecret(oauthProperties.getClientId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", oauthProperties.getClientId());
        params.add("client_secret", clientSecret);
        params.add("code", authorizationCode);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<AppleTokenResponse> response = restTemplate.postForEntity(
                OAuthConstants.APPLE_TOKEN_URL, request, AppleTokenResponse.class
        );
        return response.getBody();
    }

    public void revokeToken(String token) {
        List<String> clientIds = List.of(
                oauthProperties.getClientId(),
                oauthProperties.getServiceId()
        );

        for (String clientId : clientIds) {
            try {
                doRevokeToken(token, clientId);
                return; // 성공 시 즉시 반환
            } catch (Exception e) {
                log.warn("Apple token revoke failed with clientId {}: {}", clientId, e.getMessage());
            }
        }
    }

    private void doRevokeToken(String token, String clientId) {
        String clientSecret = createAppleClientSecret(clientId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("token", token);
        params.add("token_type_hint", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        restTemplate.postForEntity(OAuthConstants.APPLE_REVOKE_URL, request, String.class);
    }

    private PrivateKey loadPrivateKey() {
        try {
            String cleaned = oauthProperties.getPrivateKey()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleaned);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Apple .p8 private key 로드 실패", e);
        }
    }

}
