package com.scrim_pds.config;

import com.scrim_pds.exception.UnauthorizedException;
import com.scrim_pds.model.User;
import com.scrim_pds.service.UserService;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserService userService;

    public AuthUserArgumentResolver(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // Solo se activa si el parámetro tiene la anotación @AuthUser
        return parameter.getParameterAnnotation(AuthUser.class) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) { // <-- QUITAR 'throws Exception'
        
        // 1. Obtener el token del header "Authorization: Bearer <token>"
        String authHeader = webRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Header 'Authorization: Bearer <token>' faltante o mal formado.");
        }
        
        String token = authHeader.substring(7); // Quita "Bearer "

        // 2. Buscar al usuario (ya no lanza IOException)
        User user = userService.findUserByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Token inválido o expirado."));

        return user;
    }
}