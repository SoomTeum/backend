package com.comma.soomteum.domain.auth.annotation;

import com.comma.soomteum.domain.auth.JwtTokenManager;
import com.comma.soomteum.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenManager jwtTokenManager;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasLoginUserAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        boolean hasUserType = User.class.isAssignableFrom(parameter.getParameterType());
        return hasLoginUserAnnotation && hasUserType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String token = jwtTokenManager.resolveToken(request);
        if (token == null || !jwtTokenManager.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        return jwtTokenManager.getUserIdFromToken(token);
    }
}
