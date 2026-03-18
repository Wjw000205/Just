package org.example.just.interceptor;



import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.just.context.UserContext;
import org.example.just.utils.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求（预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 Token
        String token = request.getHeader("Authorization");

        // 如果没有 Token，返回 401
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
            return false;
        }

        // 移除 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证 Token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return false;
        }

        // Token 验证通过，将用户信息存入请求头供后续使用
        Integer userId = jwtUtil.getUserIdFromToken(token);
        String userName = jwtUtil.getUserNameFromToken(token);
        Integer role = jwtUtil.getRoleFromToken(token);

        // 存入 ThreadLocal
        UserContext.setUserInfo(userId, userName, role);

        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清除 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
