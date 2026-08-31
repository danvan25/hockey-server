package com.example.hockeyserver.security;

import com.example.hockeyserver.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            JwtAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(
                AUTHORIZATION_HEADER
        );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext()
                .getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(
                BEARER_PREFIX.length()
        ).trim();

        if (token.isEmpty()) {
            rejectRequest(request, response, null);
            return;
        }

        try {
            Claims claims = jwtService.parseToken(token);
            AuthenticatedUser authenticatedUser =
                    createAuthenticatedUser(claims);

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + authenticatedUser.role().name()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            List.of(authority)
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            rejectRequest(request, response, exception);
        }
    }

    private AuthenticatedUser createAuthenticatedUser(Claims claims) {
        String username = claims.getSubject();
        String email = claims.get("email", String.class);
        String roleName = claims.get("role", String.class);
        Object userIdClaim = claims.get("userId");

        if (username == null || username.isBlank()
                || email == null || email.isBlank()
                || roleName == null
                || !(userIdClaim instanceof Number userIdNumber)) {
            throw new IllegalArgumentException(
                    "Required JWT claims are missing"
            );
        }

        return new AuthenticatedUser(
                userIdNumber.longValue(),
                username,
                email,
                Role.valueOf(roleName)
        );
    }

    private void rejectRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception cause
    ) throws IOException {
        InsufficientAuthenticationException exception =
                new InsufficientAuthenticationException(
                        "Invalid access token",
                        cause
                );

        authenticationEntryPoint.commence(
                request,
                response,
                exception
        );
    }
}
