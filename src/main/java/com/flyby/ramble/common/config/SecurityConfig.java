package com.flyby.ramble.common.config;

import com.flyby.ramble.auth.filter.JwtFilter;
import com.flyby.ramble.common.properties.SecurityHttpProperties;
import com.flyby.ramble.oauth.handler.OidcAuthenticationSuccessHandler;
import com.flyby.ramble.oauth.service.CustomOidcUserService;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityHttpProperties securityHttpProperties;
    private final JwtFilter jwtFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        String[] permitPaths = securityHttpProperties.getPermitPaths()
                .stream()
                .map(String::trim)
                .toArray(String[]::new);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher(new NegatedRequestMatcher(oauth2PathsMatcher()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitPaths).permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http,
                                                   CustomOidcUserService customOidcUserService,
                                                   OidcAuthenticationSuccessHandler successHandler) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher(oauth2PathsMatcher())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(redir -> redir.baseUri("/oauth2/callback/*"))
                        .userInfoEndpoint(info -> info.oidcUserService(customOidcUserService))
                        .successHandler(successHandler))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(securityHttpProperties.getAllowedOrigins());
        config.setAllowedMethods(securityHttpProperties.getAllowedMethods());
        config.setAllowedHeaders(securityHttpProperties.getAllowedHeaders());
        config.setAllowCredentials(securityHttpProperties.isAllowCredentials());
        config.setMaxAge(securityHttpProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private RequestMatcher oauth2PathsMatcher() {
        return new OrRequestMatcher(
                securityHttpProperties.getOAuth2Paths()
                        .stream()
                        .map(AntPathRequestMatcher::new)
                        .toArray(AntPathRequestMatcher[]::new)
        );
    }

}
