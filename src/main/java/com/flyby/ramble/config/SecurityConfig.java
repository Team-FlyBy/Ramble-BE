package com.flyby.ramble.config;

import com.flyby.ramble.auth.filter.JwtFilter;
import com.flyby.ramble.auth.handler.OidcAuthenticationSuccessHandler;
import com.flyby.ramble.auth.service.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomOidcUserService customOidcUserService,
                                                   OidcAuthenticationSuccessHandler successHandler) throws Exception {
        http.cors(cors -> cors
                .configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(Collections.singletonList("*"));   // TODO : 추후 FE로 변경
                    config.addAllowedMethod(CorsConfiguration.ALL);             // TODO : 추후 변경
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(Collections.singletonList("*"));
                    config.setMaxAge(3600L);

                    return config;
                }));

        // TODO : csrf, authorization 리스트 설정 변경
        String[] list = {"/oauth2/authorize/**", "/oauth2/callback/**", "/api-docs/**", "/swagger-ui/**", "/v3/api-docs/**"};

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(list))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(list).permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(redir -> redir.baseUri("/oauth2/callback/*"))
                        .userInfoEndpoint(info -> info.oidcUserService(customOidcUserService))
                        .successHandler(successHandler))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

}
