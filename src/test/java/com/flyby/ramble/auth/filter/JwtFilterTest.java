package com.flyby.ramble.auth.filter;

import com.flyby.ramble.auth.dto.JwtTokenRequest;
import com.flyby.ramble.auth.service.AuthService;
import com.flyby.ramble.auth.util.JwtUtil;
import com.flyby.ramble.common.constants.JwtConstants;
import com.flyby.ramble.common.model.DeviceType;
import com.flyby.ramble.common.model.OAuthProvider;
import com.flyby.ramble.common.properties.JwtProperties;
import com.flyby.ramble.common.properties.SecurityHttpProperties;
import com.flyby.ramble.user.model.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("JwtFilter 테스트")
@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    AuthService authService;

    @Mock
    SecurityHttpProperties securityHttpProperties;

    @Mock
    JwtProperties jwtProperties;

    @InjectMocks
    JwtUtil jwtUtil;

    JwtFilter jwtFilter;

    JwtTokenRequest tokenRequest;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(securityHttpProperties, authService, jwtUtil);

        tokenRequest = new JwtTokenRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Role.ROLE_USER,
                DeviceType.ANDROID,
                OAuthProvider.GOOGLE,
                "1223456"
        );

        given(jwtProperties.getSecret()).willReturn("TestJwtSecretKeyForUnitTestingOnlyMP0EqLD3I=");
    }

    @DisplayName("JWT 필터 테스트 - 유효한 토큰")
    @Test
    void doFilterInternal_validToken() throws Exception {
        given(jwtProperties.getIssuer()).willReturn("test-issuer");
        given(jwtProperties.getAccessExpiration()).willReturn(3600000L);
        jwtUtil.init();

        // given
        String token = jwtUtil.generateAccToken(tokenRequest);

        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(HttpHeaders.AUTHORIZATION, JwtConstants.TOKEN_PREFIX + token);
        FilterChain filterChain = mock(FilterChain.class);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        assertThat(userDetails.getUsername()).isEqualTo(tokenRequest.userExternalId().toString());
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo(tokenRequest.role().name());

        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    @DisplayName("JWT 필터 테스트 - 토큰 X")
    @Test
    void doFilterInternal_noToken() throws Exception {
        jwtUtil.init();

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
        given(jwtProperties.getAccessExpiration()).willReturn(3600000L);
        jwtUtil.init();

        // given
        String token = jwtUtil.generateAccToken(tokenRequest);

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