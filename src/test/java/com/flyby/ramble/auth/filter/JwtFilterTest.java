package com.flyby.ramble.auth.filter;

import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.user.model.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtUtil.class)
@TestPropertySource(properties = {
        "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "jwt.issuer=test-issuer",
        "jwt.expiration-ms.access=3600000",   // 1시간
        "jwt.expiration-ms.refresh=604800000" // 7일
})
class JwtFilterTest {

    @Autowired
    JwtUtil jwtUtil;

    JwtFilter jwtFilter;

    String userId;
    Role role;
    DeviceType deviceType;
    OAuthProvider provider;
    String providerId;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil);
        userId = UUID.randomUUID().toString();
        role = Role.ROLE_USER;
        deviceType = DeviceType.ANDROID;
        provider = OAuthProvider.GOOGLE;
        providerId = "1223456";
    }

    @DisplayName("JWT 필터 테스트 - 유효한 토큰")
    @Test
    void doFilterInternal_validToken() throws Exception {
        // given
        String token = jwtUtil.generateAccToken(userId, role, deviceType, provider, providerId);

        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        FilterChain filterChain = mock(FilterChain.class);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userId);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo(role.name());

        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    @DisplayName("JWT 필터 테스트 - 토큰 X")
    @Test
    void doFilterInternal_noToken() throws Exception {
        // given
        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("Missing Access Token");

        verify(filterChain, never()).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    @DisplayName("JWT 필터 테스트 - 잘못된 토큰")
    @Test
    void doFilterInternal_invalidToken() throws Exception {
        // given
        String token = jwtUtil.generateAccToken(userId, role, deviceType, provider, providerId);

        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(HttpHeaders.AUTHORIZATION, token); // Bearer 없이 토큰만 추가
        FilterChain filterChain = mock(FilterChain.class);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("Invalid Access Token");

        verify(filterChain, never()).doFilter(request, response);
        SecurityContextHolder.clearContext();

    }

}