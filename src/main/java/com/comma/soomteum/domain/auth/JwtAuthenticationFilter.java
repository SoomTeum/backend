package com.comma.soomteum.domain.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * 기본값(true)을 뒤집어서, 비동기 디스패치 시에도 필터가 실행되도록 합니다.
     * (초기 요청과 async 재디스패치 모두에서 인증 상태를 보장)
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * 에러 디스패치에서도 토큰을 읽어 컨텍스트를 세팅할 수 있게 합니다.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        // 이미 다른 필터/디스패치에서 인증된 경우 중복 작업을 피함
        var context = SecurityContextHolder.getContext();
        var existingAuth = context.getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated()) {
            if (log.isDebugEnabled()) {
                log.debug("[JWT] Skip auth: already authenticated principal={}, dispatchType={}, thread={}",
                        existingAuth.getName(), request.getDispatcherType(), Thread.currentThread().getName());
            }
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (!StringUtils.hasText(token)) {
            if (log.isDebugEnabled()) {
                log.debug("[JWT] No token. uri={}, method={}, dispatchType={}, thread={}",
                        request.getRequestURI(), request.getMethod(),
                        request.getDispatcherType(), Thread.currentThread().getName());
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtTokenManager.validateToken(token)) {
            Authentication authentication = jwtTokenManager.getAuthentication(token);
            if (authentication != null) {
                context.setAuthentication(authentication);
                if (log.isDebugEnabled()) {
                    log.debug("[JWT] Authenticated principal={}, authorities={}, uri={}, dispatchType={}, thread={}",
                            authentication.getName(), authentication.getAuthorities(),
                            request.getRequestURI(), request.getDispatcherType(), Thread.currentThread().getName());
                }
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("[JWT] getAuthentication returned null. uri={}, dispatchType={}",
                            request.getRequestURI(), request.getDispatcherType());
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[JWT] Invalid token. uri={}, dispatchType={}, thread={}",
                        request.getRequestURI(), request.getDispatcherType(), Thread.currentThread().getName());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // 헤더 로깅은 디버그에서만
        if (log.isTraceEnabled()) {
            log.trace("[JWT] Authorization header present? {} (uri={})",
                    (bearerToken != null), request.getRequestURI());
        }
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
