package com.justeam.rdp.dataset;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.security.UserAccountService;
import com.justeam.rdp.security.UserPrincipal;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class DatasetExportTicketService {
    private static final Duration TTL=Duration.ofSeconds(90);
    private final StringRedisTemplate redis;private final JsonSupport json;private final UserAccountService users;private final AuditService audit;private final SecureRandom random=new SecureRandom();
    public DatasetExportTicketService(StringRedisTemplate redis,JsonSupport json,UserAccountService users,AuditService audit){this.redis=redis;this.json=json;this.users=users;this.audit=audit;}
    public Issued issue(UserPrincipal user,ExportRequest request,String clientAddress){byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),digest=sha(token),session=activeSession(user.id());Ticket ticket=new Ticket(user.id(),user.username(),session,request,clientAddress);try{redis.opsForValue().set(key(digest),json.write(ticket),TTL);}catch(Exception ex){throw new BusinessException(503,"导出下载凭证服务暂不可用");}audit.recordIndependent(user.id(),user.username(),"EXPORT_TICKET_ISSUED","DATASET","签发一次性流式下载凭证",Map.of("datasetId",request.datasetId(),"includeAttachments",request.includeAttachments(),"expiresIn",TTL.toSeconds(),"ticketId",digest.substring(0,16)));return new Issued(token,TTL.toSeconds());}
    public Authorized consume(String token,String clientAddress){if(token==null||!token.matches("^[A-Za-z0-9_-]{43}$"))throw BusinessException.unauthorized("下载凭证无效");String digest=sha(token),raw;try{raw=redis.opsForValue().getAndDelete(key(digest));}catch(Exception ex){throw new BusinessException(503,"导出下载凭证服务暂不可用");}if(raw==null)throw BusinessException.unauthorized("下载凭证已过期或已使用");Ticket ticket;try{ticket=json.mapper().readValue(raw,Ticket.class);}catch(Exception ex){throw BusinessException.unauthorized("下载凭证损坏");}if(ticket.clientAddress()!=null&&!ticket.clientAddress().equals(clientAddress))deny(ticket,digest,"下载凭证客户端不匹配");String active=activeSession(ticket.userId());if(!MessageDigest.isEqual(active.getBytes(java.nio.charset.StandardCharsets.UTF_8),ticket.sessionJti().getBytes(java.nio.charset.StandardCharsets.UTF_8)))deny(ticket,digest,"登录会话已失效");UserPrincipal user=users.loadById(ticket.userId());if(!user.enabled()||!user.permissions().contains("dataset:export")||ticket.request().includeAttachments()&&!user.permissions().contains("file:read"))deny(ticket,digest,"当前账号已无导出权限");return new Authorized(user,ticket.request(),digest.substring(0,16));}
    private String activeSession(long userId){try{String value=redis.opsForValue().get("session:"+userId);if(value==null||value.isBlank())throw BusinessException.unauthorized("登录会话已失效");return value;}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new BusinessException(503,"导出下载凭证服务暂不可用");}}
    private void deny(Ticket ticket,String digest,String message){audit.recordIndependent(ticket.userId(),ticket.username(),"EXPORT_FAILED","DATASET","一次性导出下载凭证校验失败",Map.of("datasetId",ticket.request().datasetId(),"stage","TICKET_CONSUME","ticketId",digest.substring(0,16),"result","FAILED"));if(message.contains("权限"))throw BusinessException.forbidden(message);throw BusinessException.unauthorized(message);}
    private String key(String digest){return "export:ticket:"+digest;}
    private String sha(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    public record ExportRequest(long datasetId,String format,List<String> fields,boolean includeAttachments){}
    private record Ticket(long userId,String username,String sessionJti,ExportRequest request,String clientAddress){}
    public record Authorized(UserPrincipal user,ExportRequest request,String ticketId){}
    public record Issued(String token,long expiresIn){}
}
