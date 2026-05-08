package com.example.kserverproject.common.config.security;

import com.example.kserverproject.common.jwt.CustomUserDetailsService;
import com.example.kserverproject.common.jwt.JwtFilter;
import com.example.kserverproject.common.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final StringRedisTemplate stringRedisTemplate;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/signup", "/api/auth/login" ).permitAll()
                        .requestMatchers("/api/menus/**").permitAll()
                        // 어드민 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 고객 전용 (고객만 주문 생성 가능)
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtFilter(jwtUtil, customUserDetailsService, stringRedisTemplate),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
