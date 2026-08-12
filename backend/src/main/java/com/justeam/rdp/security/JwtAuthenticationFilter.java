package com.justeam.rdp.security;

import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final UserAccountService users;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(TokenService tokens, UserAccountService users, ObjectMapper mapper) {
        this.tokens = tokens;
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return "POST".equalsIgnoreCase(request.getMethod())&&path.startsWith("/api/integrations/webhook/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            TokenService.Claims claims = tokens.validateAccess(authorization.substring(7));
            UserPrincipal principal = users.loadById(claims.userId());
            if (!principal.enabled()) {
                tokens.revoke(principal.id());
                throw BusinessException.unauthorized("账号已停用");
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            if (principal.mustChangePassword() && !passwordChangeAllowed(request)) {
                response.setStatus(403);response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                mapper.writeValue(response.getOutputStream(),ApiResponse.error(403,"首次登录必须修改引导密码"));return;
            }
            chain.doFilter(request, response);
        } catch (BusinessException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(401);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ApiResponse.error(401, ex.getMessage()));
        }
    }

    private boolean passwordChangeAllowed(HttpServletRequest request){
        String path=request.getRequestURI();
        return path.equals("/api/auth/logout")||path.equals("/api/user/password")||
                (path.equals("/api/user/profile")&&request.getMethod().equals("GET"));
    }
}
