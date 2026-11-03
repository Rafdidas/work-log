package com.worklog.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** SecurityConfig에서만 생성한다(빈으로 등록하면 서블릿 필터로 중복 등록됨). */
public class JwtCookieFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "WL_TOKEN";

    private final JwtService jwtService;

    public JwtCookieFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    jwtService.parse(cookie.getValue()).ifPresent(userId -> SecurityContextHolder.getContext()
                            .setAuthentication(new UsernamePasswordAuthenticationToken(
                                    new CurrentUser(userId), null, List.of())));
                }
            }
        }
        chain.doFilter(request, response);
    }
}
