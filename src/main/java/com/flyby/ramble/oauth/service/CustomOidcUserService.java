package com.flyby.ramble.oauth.service;

import com.flyby.ramble.oauth.model.CustomOidcUser;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Status;
import com.flyby.ramble.user.model.User;
import com.flyby.ramble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String subject  = oidcUser.getSubject();
        String email    = oidcUser.getEmail();
        String username = oidcUser.getFullName();

        if (email == null || username == null) {
            throw new OAuth2AuthenticationException("필수 정보(email, name)가 누락되었습니다.");
        }

        User user = userService.registerOrLogin(email, username, OAuthProvider.from(provider), subject);

        if (user == null || user.getStatus() != Status.ACTIVE) {
            throw new OAuth2AuthenticationException("사용자 정보가 유효하지 않습니다.");
        }

        return new CustomOidcUser(
                Set.of(new SimpleGrantedAuthority(user.getRole().name())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                user
        );
    }
}
