package com.renthouse.security;

import com.renthouse.enums.OperatorRole;
import com.renthouse.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String principalType = jwtUtil.extractPrincipalType(token);
                AuthenticatedUser principal = null;

                if ("OPERATOR".equals(principalType)) {
                    Long operatorId = jwtUtil.extractOperatorId(token);
                    String role = jwtUtil.extractOperatorRole(token);
                    if (operatorId != null && role != null) {
                        principal = AuthenticatedUser.forOperator(operatorId, OperatorRole.valueOf(role));
                    }
                } else {
                    Long userId = jwtUtil.extractUserId(token);
                    Long accountId = jwtUtil.extractAccountId(token);
                    if (userId != null && accountId != null) {
                        principal = AuthenticatedUser.forUser(userId, accountId);
                    }
                }

                if (principal != null) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
