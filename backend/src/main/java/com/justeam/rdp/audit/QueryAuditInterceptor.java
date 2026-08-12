package com.justeam.rdp.audit;

import com.justeam.rdp.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class QueryAuditInterceptor implements HandlerInterceptor {
    private static final String START=QueryAuditInterceptor.class.getName()+".start";
    private final AuditService audit;
    public QueryAuditInterceptor(AuditService audit){this.audit=audit;}

    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
        request.setAttribute(START,System.nanoTime());return true;
    }

    @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception ex){
        if(skip(request.getRequestURI()))return;
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null||!(authentication.getPrincipal() instanceof UserPrincipal user))return;
        long started=request.getAttribute(START) instanceof Long value?value:System.nanoTime();
        Map<String,Object> details=new LinkedHashMap<>();details.put("path",request.getRequestURI());
        details.put("parameterNames",request.getParameterMap().keySet().stream().sorted().toList());
        details.put("responseStatus",response.getStatus());details.put("elapsedMs",(System.nanoTime()-started)/1_000_000);
        details.put("result",ex==null&&response.getStatus()<400?"COMPLETED":"FAILED");
        if("GET".equals(request.getMethod())){
            audit.recordAs(user.id(),user.username(),"QUERY",module(request.getRequestURI()),"查询平台数据",details);
        }else if(response.getStatus()>=400&&Set.of("POST","PUT","PATCH","DELETE").contains(request.getMethod())){
            details.put("method",request.getMethod());details.put("exceptionType",ex==null?null:ex.getClass().getSimpleName());
            audit.recordAs(user.id(),user.username(),"WRITE_FAILED",module(request.getRequestURI()),"平台写操作失败",details);
        }
    }

    private boolean skip(String path){return path.startsWith("/api/audits")||path.equals("/api/dashboard/search")||path.contains("/export");}
    private String module(String path){String[] parts=path.split("/");String value=parts.length>2?parts[2]:"SYSTEM";return switch(value){case "datasets"->"DATASET";case "templates"->"TEMPLATE";case "trace"->"TRACE";case "devices"->"DEVICE";case "files"->"FILE";case "integrations"->"INTEGRATION";case "dashboard"->"DASHBOARD";case "admin"->"ADMIN";case "user"->"USER";default->value.toUpperCase(Locale.ROOT);};}
}
