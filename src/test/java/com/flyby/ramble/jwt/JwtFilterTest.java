package com.flyby.ramble.jwt;

import com.flyby.ramble.model.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtUtil.class)
@TestPropertySource(properties = {
        "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "jwt.issuer=test-issuer",
        "jwt.expiration-ms=1800000"  // 30분
})
class JwtFilterTest {

    @Autowired
    JwtUtil jwtUtil;

    JwtFilter jwtFilter;

    UUID userId;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil);
        userId = UUID.randomUUID();
    }

    @DisplayName("JWT 필터 테스트 - 유효한 토큰")
    @Test
    void doFilterInternal_validToken() throws Exception {
        // given
        String token = jwtUtil.createToken(userId, Role.USER, "testTenantId");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        // 필터 체인이 호출되었는지 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userId.toString());
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo(Role.USER.name());
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("JWT 필터 테스트 - 토큰 X")
    @Test
    void doFilterInternal_noToken() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        // 필터 체인이 호출되었는지 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
    }

}