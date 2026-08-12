package com.justeam.rdp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.RequestBodyTooLargeException;
import com.justeam.rdp.security.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounds anonymous integration events before MVC materializes their raw JSON for HMAC verification. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class IntegrationWebhookRequestSizeFilter extends OncePerRequestFilter {
    static final int MAX_BODY_BYTES=1_048_576;
    private static final String WEBHOOK_PREFIX="/api/integrations/webhook/";
    private final ObjectMapper mapper;private final IntegrationWebhookAbuseGuard guard;private final ClientIpResolver clientIps;private final AuditService audit;
    public IntegrationWebhookRequestSizeFilter(ObjectMapper mapper,IntegrationWebhookAbuseGuard guard,ClientIpResolver clientIps,AuditService audit){this.mapper=mapper;this.guard=guard;this.clientIps=clientIps;this.audit=audit;}

    @Override protected boolean shouldNotFilter(HttpServletRequest request){
        String uri=request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equalsIgnoreCase(request.getMethod())||!uri.startsWith(WEBHOOK_PREFIX);
    }

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        long started=System.nanoTime();String requestId=UUID.randomUUID().toString();response.setHeader("X-Request-ID",requestId);
        String uri=request.getRequestURI().substring(request.getContextPath().length()),systemCode=uri.substring(WEBHOOK_PREFIX.length());int separator=systemCode.indexOf('/');if(separator>=0)systemCode=systemCode.substring(0,separator);separator=systemCode.indexOf(';');if(separator>=0)systemCode=systemCode.substring(0,separator);
        String clientIp=clientIps.resolve(request);if(request.getContentLengthLong()>MAX_BODY_BYTES){guard.bodyTooLarge(systemCode,clientIp);byte[] rejected=reject(response);auditCall(requestId,systemCode,clientIp,null,null,request.getContentLengthLong(),null,response.getStatus(),rejected,started);return;}
        try{guard.check(systemCode,clientIp);}catch(BusinessException ex){byte[] rejected=reject(response,ex.code(),ex.getMessage());auditCall(requestId,systemCode,clientIp,null,null,request.getContentLengthLong(),null,response.getStatus(),rejected,started);return;}
        BodyEvidence requestEvidence=new BodyEvidence();String auditedSystem=systemCode,auditedIp=clientIp;ContentCachingResponseWrapper cached=new ContentCachingResponseWrapper(response);boolean completed=false;try{chain.doFilter(new LimitedRequest(request,()->guard.bodyTooLarge(auditedSystem,auditedIp),requestEvidence),cached);completed=true;}finally{byte[] responseBody=cached.getContentAsByteArray();Object authType=request.getAttribute("rdp.integration.authType"),jobId=request.getAttribute("rdp.integration.jobId");auditCall(requestId,systemCode,clientIp,authType==null?null:String.valueOf(authType),jobId,requestEvidence.size(),requestEvidence.digest(),completed?cached.getStatus():500,responseBody,started);cached.copyBodyToResponse();}
    }

    private void auditCall(String requestId,String systemCode,String clientIp,String authType,Object jobId,long requestBytes,String requestDigest,int status,byte[] responseBody,long started){try{Map<String,Object> details=new LinkedHashMap<>();details.put("requestId",requestId);details.put("method","POST");details.put("path","/api/integrations/webhook/{systemCode}");details.put("systemCodeDigest",digest((systemCode==null?"":systemCode).getBytes(StandardCharsets.UTF_8)));details.put("clientIpDigest",digest((clientIp==null?"unknown":clientIp).getBytes(StandardCharsets.UTF_8)));if(authType!=null)details.put("authType",authType);if(jobId!=null)details.put("jobId",jobId);details.put("requestBytes",Math.max(0,requestBytes));if(requestDigest!=null)details.put("requestDigest",requestDigest);details.put("responseStatus",status);details.put("responseBytes",responseBody==null?0:responseBody.length);if(responseBody!=null)details.put("responseDigest",digest(responseBody));details.put("durationMs",Math.max(0,(System.nanoTime()-started)/1_000_000));audit.recordIndependent(null,"anonymous","INTEGRATION_API_CALL","INTEGRATION","外部集成API调用安全摘要",details);}catch(Exception ignored){/* Audit evidence failure must not replace the authenticated API result. */}}
    private static String digest(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception ex){throw new IllegalStateException(ex);}}

    private byte[] reject(HttpServletResponse response)throws IOException{
        return reject(response,HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,"外部系统事件超过1MB限制");
    }
    private byte[] reject(HttpServletResponse response,int code,String message)throws IOException{byte[] body=mapper.writeValueAsBytes(ApiResponse.error(code,message));response.setStatus(code);response.setCharacterEncoding(StandardCharsets.UTF_8.name());response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.setContentLength(body.length);response.getOutputStream().write(body);return body;}

    private static final class LimitedRequest extends HttpServletRequestWrapper{
        private final Runnable onLimit;private final BodyEvidence evidence;
        private LimitedRequest(HttpServletRequest request,Runnable onLimit,BodyEvidence evidence){super(request);this.onLimit=onLimit;this.evidence=evidence;}
        @Override public ServletInputStream getInputStream()throws IOException{return new LimitedInputStream(super.getInputStream(),onLimit,evidence);}
        @Override public BufferedReader getReader()throws IOException{return new BufferedReader(new InputStreamReader(getInputStream(),getCharacterEncoding()==null?StandardCharsets.UTF_8:java.nio.charset.Charset.forName(getCharacterEncoding())));}
    }
    private static final class LimitedInputStream extends ServletInputStream{
        private final ServletInputStream delegate;private final Runnable onLimit;private final BodyEvidence evidence;private long consumed;private boolean reported;
        private LimitedInputStream(ServletInputStream delegate,Runnable onLimit,BodyEvidence evidence){this.delegate=delegate;this.onLimit=onLimit;this.evidence=evidence;}
        @Override public int read()throws IOException{int value=delegate.read();if(value>=0){evidence.update((byte)value);if(++consumed>MAX_BODY_BYTES)throw tooLarge();}return value;}
        @Override public int read(byte[] bytes,int offset,int length)throws IOException{int allowed=(int)Math.min(length,Math.max(1,MAX_BODY_BYTES-consumed+1));int read=delegate.read(bytes,offset,allowed);if(read>0){evidence.update(bytes,offset,read);if((consumed+=read)>MAX_BODY_BYTES)throw tooLarge();}return read;}
        private RequestBodyTooLargeException tooLarge(){if(!reported){reported=true;onLimit.run();}return new RequestBodyTooLargeException("外部系统事件超过1MB限制");}
        @Override public boolean isFinished(){return delegate.isFinished();}@Override public boolean isReady(){return delegate.isReady();}@Override public void setReadListener(ReadListener listener){delegate.setReadListener(listener);}
    }
    private static final class BodyEvidence{private final MessageDigest digest;private long size;private BodyEvidence(){try{digest=MessageDigest.getInstance("SHA-256");}catch(Exception ex){throw new IllegalStateException(ex);}}private void update(byte value){digest.update(value);size++;}private void update(byte[] bytes,int offset,int length){digest.update(bytes,offset,length);size+=length;}private long size(){return size;}private String digest(){return size==0?null:HexFormat.of().formatHex(digest.digest());}}
}
