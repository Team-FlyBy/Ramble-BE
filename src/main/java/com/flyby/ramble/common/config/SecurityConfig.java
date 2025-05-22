package com.flyby.ramble.common.config;

import com.flyby.ramble.auth.filter.JwtFilter;
import com.flyby.ramble.auth.handler.OidcAuthenticationSuccessHandler;
import com.flyby.ramble.auth.service.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher excluded = new OrRequestMatcher(
                new AntPathRequestMatcher("/oauth2/authorize/**"),
                new AntPathRequestMatcher("/oauth2/callback/**"),
                new AntPathRequestMatcher("/api-docs/**"),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/v3/api-docs/**")
        );

        http
                .securityMatcher(new NegatedRequestMatcher(excluded))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http,
                                                   CustomOidcUserService customOidcUserService,
                                                   OidcAuthenticationSuccessHandler successHandler) throws Exception {
        http
                .securityMatcher(
                        new OrRequestMatcher(
                                new AntPathRequestMatcher("/oauth2/authorize/**"),
                                new AntPathRequestMatcher("/oauth2/callback/**")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(redir -> redir.baseUri("/oauth2/callback/*"))
                        .userInfoEndpoint(info -> info.oidcUserService(customOidcUserService))
                        .successHandler(successHandler))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

}
