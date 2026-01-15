package com.comma.soomteum.config;

import com.comma.soomteum.domain.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity(debug = false)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 접근 허용
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/auth/**",
                                "/error",
                                "/api/places/**",           // 여행지 조회
                                "/api/parking/**",          // 주차장 조회
                                "/api/kor/**",              // 관광정보 API
                                "/api/tour/**",             // 여행 추천
                                "/api/admin/cache/**",      // 캐시 통계
                                "/actuator/**"              // Actuator
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/**").permitAll() // 사용자 정보 조회
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // 프리플라이트 허용
                        
                        // 인증 필요 (좋아요, 저장 기능만)
                        .requestMatchers("/api/places/*/like").authenticated()        // 좋아요
                        .requestMatchers("/api/places/*/save").authenticated()        // 저장
                        .requestMatchers("/api/user/places/**").authenticated()       // 나의 여행지
                        .requestMatchers(HttpMethod.POST, "/api/user/**").authenticated()   // 사용자 정보 수정
                        .requestMatchers(HttpMethod.PUT, "/api/user/**").authenticated()    // 사용자 정보 수정
                        .requestMatchers(HttpMethod.DELETE, "/api/user/**").authenticated() // 사용자 정보 삭제
                        
                        .anyRequest().permitAll()  // 나머지는 모두 허용
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(f -> f.sameOrigin()));

        return http.build();
    }
}
