package com.justeam.rdp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Issues and atomically consumes request-bound, one-time secondary-authentication proofs. */
@Service
public class StepUpService {
    public static final String HEADER="X-Step-Up-Token";
    private static final Duration TTL=Duration.ofMinutes(5);
    private static final Base64.Encoder TOKEN_ENCODER=Base64.getUrlEncoder().withoutPadding();
    private final StringRedisTemplate redis;
    private final JdbcClient jdbc;
    private final ObjectMapper mapper;
    private final SecondaryCredentialService secondaryCredentials;
    private final AuditService audit;
    private final SecureRandom random=new SecureRandom();

    public StepUpService(StringRedisTemplate redis,JdbcClient jdbc,ObjectMapper mapper,
                         SecondaryCredentialService secondaryCredentials,AuditService audit){
        this.redis=redis;this.jdbc=jdbc;this.mapper=mapper;this.secondaryCredentials=secondaryCredentials;this.audit=audit;
    }

    public IssuedToken issue(UserPrincipal user,String purposeValue,String methodValue,String targetValue,String secondaryPassword,Object payload){
        Purpose purpose=purpose(purposeValue);
        String method=method(methodValue);
        String target=canonicalIssuedTarget(targetValue);
        CredentialStamp credentials=credentialStamp(user.id());
        secondaryCredentials.verify(user,secondaryPassword,credentials.secondaryHash(),"STEP_UP_"+purpose.name());
        String sessionJti=activeSession(user.id());
        byte[] bytes=new byte[32];random.nextBytes(bytes);String token=TOKEN_ENCODER.encodeToString(bytes);
        String tokenDigest=sha256(token);String tokenId=tokenDigest.substring(0,16);
        Proof proof=new Proof(user.id(),sessionJti,purpose.name(),method,target,securityStamp(credentials),payloadDigest(payload),tokenId);
        try{redis.opsForValue().set(tokenKey(tokenDigest),mapper.writeValueAsString(proof),TTL);}
        catch(Exception ex){throw unavailable();}
        audit.recordIndependent(user.id(),user.username(),"STEP_UP_ISSUED","SECURITY","签发敏感操作二级确认凭证",
                Map.of("purpose",purpose.name(),"method",method,"target",target,"tokenId",tokenId,"expiresIn",TTL.toSeconds()));
        return new IssuedToken(token,TTL.toSeconds());
    }

    public void consume(UserPrincipal user,String token,Purpose expectedPurpose,HttpServletRequest request){
        consume(user,token,expectedPurpose,request,null);
    }

    public void consume(UserPrincipal user,String token,Purpose expectedPurpose,HttpServletRequest request,Object payload){
        String target=canonicalRequest(request);String method=method(request.getMethod());
        if(token==null||!token.matches("^[A-Za-z0-9_-]{43}$"))reject(user,expectedPurpose,method,target,"MISSING_OR_MALFORMED");
        String tokenDigest=sha256(token);String raw;
        try{raw=redis.opsForValue().getAndDelete(tokenKey(tokenDigest));}
        catch(Exception ex){throw unavailable();}
        if(raw==null)reject(user,expectedPurpose,method,target,"EXPIRED_OR_REPLAYED");
        try{
            Proof proof=mapper.readValue(raw,Proof.class);
            String activeJti=activeSession(user.id());
            String securityStamp=securityStamp(credentialStamp(user.id()));
            boolean valid=proof.userId()==user.id()&&safeEquals(proof.sessionJti(),activeJti)
                    &&safeEquals(proof.purpose(),expectedPurpose.name())&&safeEquals(proof.method(),method)
                    &&safeEquals(proof.target(),target)&&safeEquals(proof.securityStampDigest(),securityStamp)
                    &&safeEquals(proof.payloadDigest(),payloadDigest(payload));
            if(!valid)reject(user,expectedPurpose,method,target,"BINDING_MISMATCH");
            audit.recordIndependent(user.id(),user.username(),"STEP_UP_CONSUMED","SECURITY","消费敏感操作二级确认凭证",
                    Map.of("purpose",expectedPurpose.name(),"method",method,"target",target,"tokenId",proof.tokenId()));
        }catch(BusinessException ex){throw ex;}
        catch(Exception ex){reject(user,expectedPurpose,method,target,"INVALID_PROOF");}
    }

    public String canonicalIssuedTarget(String value){
        if(value==null||value.isBlank()||value.length()>4096)throw BusinessException.badRequest("敏感操作目标不正确");
        try{
            URI uri=URI.create(value);
            if(uri.isAbsolute()||uri.getHost()!=null||uri.getFragment()!=null||uri.getUserInfo()!=null)
                throw BusinessException.badRequest("敏感操作目标必须是平台内部接口");
            String path=uri.getRawPath();
            if(path==null||!path.startsWith("/api/")||path.contains("\\")||path.contains(";")||path.contains("//")||path.contains(".."))
                throw BusinessException.badRequest("敏感操作目标不正确");
            return canonicalTarget(path,parseQuery(uri.getRawQuery()));
        }catch(BusinessException ex){throw ex;}
        catch(Exception ex){throw BusinessException.badRequest("敏感操作目标不正确");}
    }

    public String canonicalRequest(HttpServletRequest request){
        // Servlet parameter maps also contain form and multipart body fields.  A step-up
        // target binds the URI query only; request bodies are bound separately through
        // payloadDigest when a sensitive endpoint has a structured JSON body.
        return canonicalTarget(request.getRequestURI(),parseQuery(request.getQueryString()));
    }

    private Map<String,List<String>> parseQuery(String raw){
        Map<String,List<String>> query=new TreeMap<>();if(raw==null||raw.isBlank())return query;
        for(String pair:raw.split("&",-1)){
            String[] parts=pair.split("=",2);String key=decode(parts[0]);String value=parts.length==2?decode(parts[1]):"";
            query.computeIfAbsent(key,ignored->new ArrayList<>()).add(value);
        }
        query.values().forEach(values->values.sort(Comparator.naturalOrder()));return query;
    }

    private String canonicalTarget(String path,Map<String,List<String>> query){
        if(query.isEmpty())return path;
        List<String> pairs=new ArrayList<>();query.forEach((key,values)->{
            List<String> actual=values.isEmpty()?List.of(""):values;
            actual.forEach(value->pairs.add(encode(key)+"="+encode(value)));
        });
        return path+"?"+String.join("&",pairs);
    }

    private Purpose purpose(String value){
        try{return Purpose.valueOf(value==null?"":value.trim().toUpperCase(Locale.ROOT));}
        catch(Exception ex){throw BusinessException.badRequest("二级确认用途不正确");}
    }
    private String method(String value){
        String method=value==null?"":value.trim().toUpperCase(Locale.ROOT);
        if(!java.util.Set.of("GET","POST","PUT","PATCH","DELETE").contains(method))throw BusinessException.badRequest("请求方法不正确");
        return method;
    }
    private CredentialStamp credentialStamp(long userId){return jdbc.sql("""
            SELECT coalesce(u.secondary_password,'') secondary_password,coalesce(u.updated_time,u.created_time) security_updated_time,
                   coalesce(u.department_id,0) department_id,
                   coalesce((SELECT string_agg(ur.role_id::text,',' ORDER BY ur.role_id) FROM sys_user_role ur WHERE ur.user_id=u.id),'') role_ids,
                   coalesce((SELECT string_agg(DISTINCT rp.permission_id::text,',' ORDER BY rp.permission_id::text)
                             FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0
                             JOIN sys_role_permission rp ON rp.role_id=ur.role_id
                             JOIN sys_permission p ON p.id=rp.permission_id AND p.status=1 AND p.deleted=0
                             WHERE ur.user_id=u.id),'') permission_ids,
                   coalesce((SELECT string_agg(us.data_scope_id::text,',' ORDER BY us.data_scope_id) FROM sys_user_data_scope us WHERE us.user_id=u.id),'') scope_ids
            FROM sys_user u WHERE u.id=:id AND u.status=1 AND u.deleted=0
            """).param("id",userId).query((rs,n)->new CredentialStamp(rs.getString("secondary_password"),rs.getTimestamp("security_updated_time").toInstant(),
                    rs.getLong("department_id"),rs.getString("role_ids"),rs.getString("permission_ids"),rs.getString("scope_ids")))
            .optional().orElseThrow(()->BusinessException.unauthorized("账号不可用"));}
    private String securityStamp(CredentialStamp value){return sha256(value.secondaryHash()+"|"+value.updatedTime()+"|"+value.departmentId()+"|"+value.roleIds()+"|"+value.permissionIds()+"|"+value.scopeIds());}
    private String payloadDigest(Object value){
        if(value==null)return "";
        try{return sha256(mapper.writeValueAsString(normalizePayload(mapper.convertValue(value,Object.class))));}
        catch(Exception ex){throw BusinessException.badRequest("敏感操作请求体无法规范化");}
    }
    private Object normalizePayload(Object value){
        if(value instanceof Map<?,?> map){Map<String,Object> result=new TreeMap<>();map.forEach((key,item)->{if(item!=null)result.put(String.valueOf(key),normalizePayload(item));});return result;}
        if(value instanceof List<?> list)return list.stream().map(this::normalizePayload).toList();
        if(value instanceof String text){try{return java.time.Instant.parse(text).toString();}catch(java.time.format.DateTimeParseException ignored){return text;}}
        return value;
    }
    private String activeSession(long userId){
        try{String value=redis.opsForValue().get("session:"+userId);if(value==null||value.isBlank())throw BusinessException.unauthorized("登录会话已失效");return value;}
        catch(BusinessException ex){throw ex;}catch(Exception ex){throw unavailable();}
    }
    private void reject(UserPrincipal user,Purpose purpose,String method,String target,String reason){
        audit.recordIndependent(user.id(),user.username(),"STEP_UP_REJECTED","SECURITY","拒绝敏感操作二级确认凭证",
                Map.of("purpose",purpose.name(),"method",method,"target",target,"reason",reason));
        throw new BusinessException(428,"敏感操作二级确认无效、已过期或已使用，请重新验证");
    }
    private boolean safeEquals(String left,String right){return left!=null&&right!=null&&MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),right.getBytes(StandardCharsets.UTF_8));}
    private String sha256(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private String tokenKey(String digest){return "security:step-up:"+digest;}
    private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private String decode(String value){return URLDecoder.decode(value,StandardCharsets.UTF_8);}
    private BusinessException unavailable(){return new BusinessException(503,"二级认证凭证服务暂不可用");}

    public enum Purpose { DELETE,EXPORT,PRIVILEGE }
    public record IssuedToken(String token,long expiresIn){}
    private record Proof(long userId,String sessionJti,String purpose,String method,String target,String securityStampDigest,String payloadDigest,String tokenId){}
    private record CredentialStamp(String secondaryHash,java.time.Instant updatedTime,long departmentId,String roleIds,String permissionIds,String scopeIds){}
}
