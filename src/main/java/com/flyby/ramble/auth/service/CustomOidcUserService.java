package com.flyby.ramble.auth.service;

import com.flyby.ramble.model.OAuthProvider;
import com.flyby.ramble.model.Role;
import com.flyby.ramble.model.User;
import com.flyby.ramble.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService oidcUserService = new OidcUserService();
    private final UserService userService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = oidcUserService.loadUser(userRequest);

        OidcIdToken idToken = oidcUser.getIdToken();
        Map<String, Object> claims = new HashMap<>(idToken.getClaims());

        String registration = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider = OAuthProvider.from(registration);
        String providerId = idToken.getSubject();
        Object email = claims.get("email");
        Object username = claims.get("name");

        if (email == null || username == null) {
            throw new OAuth2AuthenticationException("필수 정보(email, name)가 누락되었습니다.");
        }

        User user = userService.registerOrLogin(email.toString(), username.toString(), provider, providerId);

        claims.put("userId", user.getExternalId());
        claims.put("provider", provider);
        claims.put("role", Role.USER);

        Set<GrantedAuthority> auths = new HashSet<>(oidcUser.getAuthorities());
        auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        OidcIdToken newIdToken = new OidcIdToken(
            idToken.getTokenValue(),
            idToken.getIssuedAt(),
            idToken.getExpiresAt(),
            claims
        );

        return new DefaultOidcUser(
            auths,
            newIdToken,
            oidcUser.getUserInfo()
        );
    }
}
