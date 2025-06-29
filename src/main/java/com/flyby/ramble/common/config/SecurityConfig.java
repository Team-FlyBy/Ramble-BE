package com.flyby.ramble.common.config;

import com.flyby.ramble.auth.filter.JwtFilter;
import com.flyby.ramble.common.properties.SecurityHttpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(securityHttpProperties.getAllowedOrigins());
        config.setAllowedMethods(securityHttpProperties.getAllowedMethods());
        config.setAllowedHeaders(securityHttpProperties.getAllowedHeaders());
        config.setExposedHeaders(securityHttpProperties.getAllowedHeaders());
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
