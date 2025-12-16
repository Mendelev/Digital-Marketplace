package com.marketplace.user.config;

import com.marketplace.user.filter.JwtValidationFilter;
import com.marketplace.user.security.AuthenticatedUser;
import com.marketplace.user.security.CurrentUser;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Web MVC configuration for custom argument resolvers.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    /**
     * Resolver for @CurrentUser annotation.
     * Extracts authenticated user from request attributes set by JwtValidationFilter.
     */
    private static class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class) &&
                   parameter.getParameterType().equals(AuthenticatedUser.class);
        }

        @Override
        public Object resolveArgument(@NonNull MethodParameter parameter,
                                     ModelAndViewContainer mavContainer,
                                     @NonNull NativeWebRequest webRequest,
                                     WebDataBinderFactory binderFactory) {
            
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            
            if (request != null) {
                return request.getAttribute(JwtValidationFilter.AUTHENTICATED_USER_ATTRIBUTE);
            }
            
            return null;
        }
    }
}
