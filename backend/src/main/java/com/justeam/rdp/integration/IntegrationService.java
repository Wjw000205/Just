package com.justeam.rdp.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.SecretCipher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PreDestroy;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class IntegrationService {
    private static final Set<String> TYPES=Set.of("MES","PLM","ERP","DEVICE","OTHER");
    private static final Set<String> AUTH_TYPES=Set.of("HMAC","OAUTH2","NONE");
    private static final Set<String> TRANSFORMS=Set.of("DIRECT","TRIM","UPPERCASE","LOWERCASE","NUMBER","DATE","CONSTANT","UNIT");
    private static final Set<String> CURSOR_TYPES=Set.of("NUMBER","INSTANT","LEXICOGRAPHIC");
    private static final Set<String> CONFLICT_POLICIES=Set.of("PRIORITY_THEN_VERSION","PRIORITY_ONLY","VERSION_ONLY");
    private static final List<Integer> DEFAULT_RETRY_DELAYS=List.of(60,300,1800);
    private final JdbcClient jdbc;private final JsonSupport json;private final SecretCipher cipher;private final AuditService audit;private final TransactionTemplate transactions;private final HttpClient http;private final ExecutorService oauthDnsExecutor;private final Set<String> deliveryAllowedHosts;private final boolean allowPrivateDeliveryHosts;private final Set<String> oauthAllowedHosts;private final boolean allowPrivateOauthHosts;

    public IntegrationService(JdbcClient jdbc,JsonSupport json,SecretCipher cipher,AuditService audit,TransactionTemplate transactions,
            @Value("${rdp.integration.delivery-allowed-hosts:}")String deliveryAllowedHosts,
            @Value("${rdp.integration.allow-private-delivery-hosts:false}")boolean allowPrivateDeliveryHosts,
            @Value("${rdp.integration.oauth-introspection-allowed-hosts:}")String oauthAllowedHosts,
            @Value("${rdp.integration.allow-private-oauth-hosts:false}")boolean allowPrivateOauthHosts){
        this.jdbc=jdbc;this.json=json;this.cipher=cipher;this.audit=audit;this.transactions=transactions;this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();this.oauthDnsExecutor=new ThreadPoolExecutor(2,4,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{Thread thread=new Thread(r,"rdp-oauth-dns");thread.setDaemon(true);return thread;},new ThreadPoolExecutor.AbortPolicy());this.deliveryAllowedHosts=hostSet(deliveryAllowedHosts);this.allowPrivateDeliveryHosts=allowPrivateDeliveryHosts;this.oauthAllowedHosts=hostSet(oauthAllowedHosts);this.allowPrivateOauthHosts=allowPrivateOauthHosts;
    }

    @PreDestroy
    void stopOauthDnsExecutor(){oauthDnsExecutor.shutdownNow();}

    /**
     * Completes the fail-closed V33 credential upgrade. The migration disables
     * previously-active HMAC integrations because SQL cannot inspect AES-GCM
     * plaintext. Only credentials that decrypt and satisfy the current strength
     * policy are restored; unsafe credentials and their pending deliveries are
     * quarantined until an administrator rotates the key.
     */
    public void validateUpgradedHmacCredentials(){
        List<Long> pending=jdbc.sql("SELECT id FROM integration_config WHERE auth_type='HMAC' AND config->>'credentialValidationRequired'='true' ORDER BY id").query(Long.class).list();
        for(Long id:pending)transactions.executeWithoutResult(status->{
            CredentialUpgrade row=jdbc.sql("SELECT id,system_code,secret_ciphertext,config::text AS config FROM integration_config WHERE id=:id AND auth_type='HMAC' FOR UPDATE").param("id",id).query(CredentialUpgrade.class).optional().orElse(null);
            if(row==null||!Boolean.TRUE.equals(json.map(row.config()).get("credentialValidationRequired")))return;
            try{
                requireStrongHmacSecret(row.secretCiphertext());
                jdbc.sql("UPDATE integration_config SET active=TRUE,config=config-'credentialValidationRequired'-'credentialUpgradeState',version=version+1,updated_time=now() WHERE id=:id").param("id",id).update();
                audit.recordAs(null,"system","INTEGRATION_CREDENTIAL_UPGRADE_VALIDATED","INTEGRATION","V33遗留HMAC凭据强度验证通过",Map.of("integrationId",id,"systemCode",row.systemCode()));
            }catch(RuntimeException ex){
                quarantineInvalidHmacCredentialLocked(id,row.systemCode());
            }
        });
    }

    public List<Map<String,Object>> configs(boolean sensitive){List<Map<String,Object>> rows=jdbc.sql("SELECT * FROM integration_config ORDER BY system_code").query(this::configRow).list();if(sensitive)return rows;for(Map<String,Object> row:rows){row.remove("baseUrl");@SuppressWarnings("unchecked") Map<String,Object> options=new LinkedHashMap<>((Map<String,Object>)row.getOrDefault("config",Map.of()));options.remove("ipWhitelist");options.remove("deliveryUrl");options.remove("oauthIntrospectionUrl");options.remove("oauthClientId");row.put("config",options);}return rows;}
    public Map<String,Object> get(long id){return jdbc.sql("SELECT * FROM integration_config WHERE id=:id").param("id",id).query(this::configRow).optional().orElseThrow(()->BusinessException.notFound("集成配置不存在"));}

    @Transactional
    public long create(ConfigBody body){
        validate(body);Map<String,Object> options=validatedOptions(body.config());validateAuthenticationOptions(body.authType(),options);requireUniqueOauthIdentity(body.authType(),options,null);String secret=secretCiphertext(body.authType(),body.secret(),null);requireDeliveryAuthentication(body.authType(),options);
        Long id=jdbc.sql("""
                INSERT INTO integration_config(system_code,system_name,system_type,base_url,auth_type,secret_ciphertext,active,config,created_by)
                VALUES (:code,:name,:type,:url,:auth,:secret,:active,CAST(:config AS jsonb),:user) RETURNING id
                """).param("code",normalizeCode(body.systemCode())).param("name",body.systemName().trim()).param("type",body.systemType().toUpperCase())
                .param("url",blank(body.baseUrl())).param("auth",body.authType().toUpperCase()).param("secret",secret).param("active",body.active())
                .param("config",json.write(options)).param("user",CurrentUser.require().id()).query(Long.class).single();
        audit.record("CREATE","INTEGRATION","创建外部系统配置",Map.of("integrationId",id,"systemCode",normalizeCode(body.systemCode()),"authType",body.authType().toUpperCase()));return id;
    }

    @Transactional
    public void update(long id,ConfigBody body){
        if(body.expectedVersion()==null)throw new BusinessException(409,"缺少配置版本，请刷新后重试");
        validate(body);Map<String,Object> options=validatedOptions(body.config());validateAuthenticationOptions(body.authType(),options);requireDeliveryAuthentication(body.authType(),options);
        Map<String,Object> existing=jdbc.sql("SELECT * FROM integration_config WHERE id=:id FOR UPDATE").param("id",id)
                .query(this::configRow).optional().orElseThrow(()->BusinessException.notFound("集成配置不存在"));
        requireUniqueOauthIdentity(body.authType(),options,id);
        if(!String.valueOf(existing.get("systemCode")).equals(normalizeCode(body.systemCode()))&&jdbc.sql("SELECT count(*) FROM integration_job WHERE integration_id=:id").param("id",id).query(Long.class).single()>0)throw new BusinessException(409,"已有接入作业的系统编码不可修改");
        String oldSecret=jdbc.sql("SELECT secret_ciphertext FROM integration_config WHERE id=:id").param("id",id).query(String.class).optional().orElse(null),oldAuth=String.valueOf(existing.get("authType"));
        boolean authChanged=!oldAuth.equalsIgnoreCase(body.authType());
        if(authChanged)oldSecret=null;
        String secret=secretCiphertext(body.authType(),body.secret(),oldSecret);
        int changed=jdbc.sql("""
                UPDATE integration_config SET system_code=:code,system_name=:name,system_type=:type,base_url=:url,auth_type=:auth,
                secret_ciphertext=:secret,active=:active,config=CAST(:config AS jsonb),version=version+1,updated_time=now()
                WHERE id=:id AND version=:version
                """).param("code",normalizeCode(body.systemCode())).param("name",body.systemName().trim()).param("type",body.systemType().toUpperCase())
                .param("url",blank(body.baseUrl())).param("auth",body.authType().toUpperCase()).param("secret",secret).param("active",body.active())
                .param("config",json.write(options)).param("id",id).param("version",body.expectedVersion()).update();
        if(changed!=1)throw new BusinessException(409,"配置已被其他用户修改，请刷新后重试");
        audit.record("UPDATE","INTEGRATION","更新外部系统配置",Map.of("integrationId",id,"systemCode",normalizeCode(body.systemCode()),"before",existing,"after",get(id)));
    }

    private void requireUniqueOauthIdentity(String authType,Map<String,Object> options,Long excludedId){
        if(!"OAUTH2".equalsIgnoreCase(authType))return;
        String issuer=String.valueOf(options.get("oauthIssuer")),sourceClient=String.valueOf(options.get("oauthSourceClientId")),identity=sha256(issuer.length()+":"+issuer+sourceClient);
        jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended(:identity,47))) oauth_identity_lock").param("identity",identity).query(Long.class).single();
        long duplicates=jdbc.sql("""
                SELECT count(*) FROM integration_config
                WHERE auth_type='OAUTH2' AND id<>:excluded
                  AND config->>'oauthIssuer'=:issuer
                  AND config->>'oauthSourceClientId'=:sourceClient
                """).param("excluded",excludedId==null?-1L:excludedId).param("issuer",issuer).param("sourceClient",sourceClient).query(Long.class).single();
        if(duplicates>0)throw new BusinessException(409,"OAuth2签发方与外部调用方AppID组合已绑定其他集成系统");
    }

    public List<Map<String,Object>> mappings(long id){get(id);return mappingRows(id);}

    @Transactional
    public void replaceMappings(long id,int expectedMappingVersion,List<MappingBody> mappings){
        Integer current=jdbc.sql("SELECT mapping_version FROM integration_config WHERE id=:id FOR UPDATE").param("id",id).query(Integer.class).optional().orElseThrow(()->BusinessException.notFound("集成配置不存在"));if(current!=expectedMappingVersion)throw new BusinessException(409,"字段映射已被其他用户修改，请刷新后重试");
        if(mappings==null)throw BusinessException.badRequest("映射列表不能为空");List<Map<String,Object>> before=mappingRows(id);
        if(mappings.stream().map(m->m.sourceField().trim()).distinct().count()!=mappings.size())throw BusinessException.badRequest("源字段不能重复");
        if(mappings.stream().map(m->m.targetField().trim()).distinct().count()!=mappings.size())throw BusinessException.badRequest("目标字段不能重复");
        for(MappingBody mapping:mappings){
            if(blank(mapping.sourceField())==null||blank(mapping.targetField())==null)throw BusinessException.badRequest("源字段和目标字段不能为空");
            String transform=normalizeTransform(mapping.transformation());
            if(("CONSTANT".equals(transform)||"UNIT".equals(transform))&&blank(mapping.defaultValue())==null)throw BusinessException.badRequest(transform+"转换必须配置常量或换算系数");
            if("UNIT".equals(transform))parseNumber(mapping.defaultValue(),"单位换算系数");
        }
        jdbc.sql("DELETE FROM integration_field_mapping WHERE integration_id=:id").param("id",id).update();
        for(MappingBody m:mappings)jdbc.sql("INSERT INTO integration_field_mapping(integration_id,source_field,target_field,transformation,required,default_value) VALUES (:id,:source,:target,:transform,:required,:default)")
                .param("id",id).param("source",m.sourceField().trim()).param("target",m.targetField().trim()).param("transform",normalizeTransform(m.transformation())).param("required",m.required()).param("default",blank(m.defaultValue())).update();
        jdbc.sql("UPDATE integration_config SET mapping_version=mapping_version+1,version=version+1,updated_time=now() WHERE id=:id").param("id",id).update();
        audit.record("MAPPING","INTEGRATION","替换字段映射",Map.of("integrationId",id,"mappingCount",mappings.size(),"before",before,"after",mappingRows(id)));
    }

    public List<Map<String,Object>> jobs(String systemCode,String status){
        String code=blank(systemCode),state=blank(status);if(state!=null&&!Set.of("RECEIVED","PROCESSING","COMPLETED","FAILED","SKIPPED","MANUAL_REVIEW").contains(state.toUpperCase()))throw BusinessException.badRequest("作业状态不正确");
        return jdbc.sql("""
                SELECT id,integration_id,system_code,direction,status,idempotency_key,record_count,error_message,error_category,
                       payload_digest,mapping_version,cursor_value,cursor_before,cursor_after,local_committed,delivery_status,
                       attempt_count,started_time,finished_time,updated_time
                FROM integration_job WHERE (CAST(:code AS text) IS NULL OR system_code=CAST(:code AS text))
                AND (CAST(:status AS text) IS NULL OR status=CAST(:status AS text)) ORDER BY started_time DESC,id DESC LIMIT 500
                """).param("code",code==null?null:code.toUpperCase()).param("status",state==null?null:state.toUpperCase()).query(this::jobRow).list();
    }

    public List<Map<String,Object>> deadLetters(String status){
        String state=blank(status);if(state!=null&&!Set.of("PENDING","RETRYING","RESOLVED","MANUAL_REVIEW").contains(state.toUpperCase()))throw BusinessException.badRequest("死信状态不正确");
        return jdbc.sql("""
                SELECT d.id,d.integration_id,c.system_code,d.job_id,d.event_key,d.error_message,d.retry_count,d.status,j.local_committed,
                       d.next_retry_time,d.last_attempt_time,d.created_time,d.resolved_time,d.resolved_by,d.resolution_note,d.version,
                       coalesce((SELECT jsonb_agg(jsonb_build_object('attemptNumber',a.attempt_number,'result',a.result,'errorCategory',a.error_category,'errorMessage',a.error_message,'mappingVersion',a.mapping_version,'mappingSnapshotDigest',a.mapping_snapshot_digest,'processingConfigDigest',a.processing_config_digest,'createdTime',a.created_time) ORDER BY a.attempt_number) FROM integration_retry_attempt a WHERE a.dead_letter_id=d.id),'[]'::jsonb)::text AS attempts
                FROM integration_dead_letter d JOIN integration_config c ON c.id=d.integration_id JOIN integration_job j ON j.id=d.job_id
                WHERE (CAST(:status AS text) IS NULL OR d.status=CAST(:status AS text)) ORDER BY d.created_time DESC,d.id DESC LIMIT 500
                """).param("status",state==null?null:state.toUpperCase()).query(this::deadLetterRow).list();
    }

    public WebhookResult acceptWebhook(String systemCode,String timestamp,String signature,String idempotencyKey,String authorization,String body,String clientIp){try{return acceptWebhookAuthenticated(systemCode,timestamp,signature,idempotencyKey,authorization,body,clientIp);}catch(BusinessException ex){Map<String,Object> evidence=Map.of("systemCodeDigest",sha256(normalizeCode(systemCode)),"clientIpDigest",sha256(clientIp==null?"unknown":clientIp));if(ex.code()==401)audit.recordIndependent(null,"anonymous","INTEGRATION_AUTH_FAILED","INTEGRATION","外部集成请求认证失败",evidence);else if(ex.code()==503)audit.recordIndependent(null,"anonymous","INTEGRATION_AUTH_PROVIDER_FAILED","INTEGRATION","外部集成认证提供方暂不可用，请求已失败关闭",evidence);throw ex;}}
    private WebhookResult acceptWebhookAuthenticated(String systemCode,String timestamp,String signature,String idempotencyKey,String authorization,String body,String clientIp){
        String code=normalizeCode(systemCode);if(blank(idempotencyKey)==null)throw BusinessException.unauthorized("缺少Webhook幂等键");
        if(idempotencyKey.length()>120)throw BusinessException.badRequest("幂等键最长120字符");
        ConfigAuth auth=jdbc.sql("SELECT id,system_code,auth_type,secret_ciphertext,active,config::text AS config,mapping_version,version FROM integration_config WHERE system_code=:code")
                .param("code",code).query(ConfigAuth.class).optional().orElseThrow(()->BusinessException.unauthorized("未知外部系统"));
        if(!auth.active())throw BusinessException.unauthorized("外部系统接入已停用");
        validateIp(auth.config(),clientIp);
        if("HMAC".equals(auth.authType()))authenticateHmac(auth,timestamp,signature,idempotencyKey,body);else if("OAUTH2".equals(auth.authType()))authenticateOauth2(auth,authorization);else throw BusinessException.unauthorized("该外部系统未启用受支持的Webhook认证方式");
        JsonNode source=parseObject(body);String digest=sha256(body);
        Receipt receipt=transactions.execute(status->persistReceipt(auth,idempotencyKey,source,digest,clientIp));
        if(receipt==null)throw new IllegalStateException("接入作业未创建");
        String finalStatus=jdbc.sql("SELECT status FROM integration_job WHERE id=:id").param("id",receipt.jobId()).query(String.class).single();
        return new WebhookResult(receipt.jobId(),finalStatus,receipt.created(),auth.authType());
    }

    private void authenticateHmac(ConfigAuth auth,String timestamp,String signature,String idempotencyKey,String body){
        if(blank(timestamp)==null||blank(signature)==null)throw BusinessException.unauthorized("缺少Webhook签名或时间戳");long epoch;try{epoch=Long.parseLong(timestamp);}catch(NumberFormatException ex){throw BusinessException.unauthorized("Webhook时间戳无效");}if(Math.abs(Instant.now().getEpochSecond()-epoch)>300)throw BusinessException.unauthorized("Webhook请求已过期");String secret;try{secret=requireStrongHmacSecret(auth.secretCiphertext());}catch(RuntimeException ex){quarantineInvalidHmacCredential(auth.id(),auth.systemCode(),auth.secretCiphertext());throw new BusinessException(503,"HMAC接入凭据需要管理员轮换");}String signingInput="RDP-HMAC-V1\nPOST\n/api/integrations/webhook/"+auth.systemCode()+"\n"+timestamp+"\n"+idempotencyKey+"\n"+body,expected=hmac(secret,signingInput),supplied=signature.startsWith("sha256=")?signature.substring(7):signature;if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),supplied.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))throw BusinessException.unauthorized("Webhook签名无效");
    }

    private void authenticateOauth2(ConfigAuth auth,String authorization){
        if(authorization==null||!authorization.regionMatches(true,0,"Bearer ",0,7))throw BusinessException.unauthorized("缺少OAuth2 Bearer令牌");
        String token=authorization.substring(7).trim();if(token.isEmpty()||token.length()>4096||token.chars().anyMatch(ch->ch<=32||ch>=127))throw BusinessException.unauthorized("OAuth2 Bearer令牌格式无效");
        Map<String,Object> options=json.map(auth.config());int timeout=intOption(options,"oauthTimeoutMillis",3000,100,5000);long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeout);URI endpoint;
        try{endpoint=validateOauthEndpointForRequest(stringOption(options,"oauthIntrospectionUrl",null),deadline);}catch(BusinessException ex){throw new BusinessException(503,"OAuth2自省端点安全校验失败");}
        String clientId=stringOption(options,"oauthClientId",null),secret=auth.secretCiphertext()==null?null:cipher.decrypt(auth.secretCiphertext());if(clientId==null||secret==null)throw new BusinessException(503,"OAuth2自省客户端配置不可用");
        String form="token="+URLEncoder.encode(token,StandardCharsets.UTF_8)+"&token_type_hint=access_token",basicMaterial=URLEncoder.encode(clientId,StandardCharsets.UTF_8)+":"+URLEncoder.encode(secret,StandardCharsets.UTF_8),basic=Base64.getEncoder().encodeToString(basicMaterial.getBytes(StandardCharsets.UTF_8));int remaining=remainingMillis(deadline);HttpRequest request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(remaining)).header("Accept","application/json").header("Content-Type","application/x-www-form-urlencoded").header("Authorization","Basic "+basic).POST(HttpRequest.BodyPublishers.ofString(form,StandardCharsets.UTF_8)).build();JsonNode result;
        try{OauthResponse response=readOauthResponse(request,remaining);if(response.statusCode()<200||response.statusCode()>=300)throw new BusinessException(503,"OAuth2令牌自省服务暂不可用");if(response.body().length>65536)throw new BusinessException(503,"OAuth2令牌自省响应超出安全上限");result=json.mapper().readTree(response.body());if(result==null||!result.isObject())throw new BusinessException(503,"OAuth2令牌自省响应格式不正确");}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new BusinessException(503,"OAuth2令牌自省服务暂不可用");}
        if(!result.path("active").isBoolean()||!result.path("active").booleanValue())throw BusinessException.unauthorized("OAuth2访问令牌无效或已撤销");long now=Instant.now().getEpochSecond();if(result.has("exp")&&(!result.path("exp").canConvertToLong()||result.path("exp").asLong()<=now))throw BusinessException.unauthorized("OAuth2访问令牌已过期");if(result.has("nbf")&&(!result.path("nbf").canConvertToLong()||result.path("nbf").asLong()>now+30))throw BusinessException.unauthorized("OAuth2访问令牌尚未生效");requireOauthClaims(options,result);
    }

    private OauthResponse readOauthResponse(HttpRequest request,int timeoutMillis){
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);CompletableFuture<HttpResponse<InputStream>> call=http.sendAsync(request,HttpResponse.BodyHandlers.ofInputStream());HttpResponse<InputStream> response;
        try{response=call.get(timeoutMillis,TimeUnit.MILLISECONDS);}catch(TimeoutException ex){call.cancel(true);throw new BusinessException(503,"OAuth2令牌自省响应超时");}catch(InterruptedException ex){call.cancel(true);Thread.currentThread().interrupt();throw new BusinessException(503,"OAuth2令牌自省被中断");}catch(Exception ex){throw new BusinessException(503,"OAuth2令牌自省服务暂不可用");}
        InputStream stream=response.body();CompletableFuture<byte[]> reading=CompletableFuture.supplyAsync(()->{try(stream){return stream.readNBytes(65537);}catch(Exception ex){throw new IllegalStateException(ex);}});long remaining=deadline-System.nanoTime();try{if(remaining<=0)throw new TimeoutException();return new OauthResponse(response.statusCode(),reading.get(remaining,TimeUnit.NANOSECONDS));}catch(TimeoutException ex){try{stream.close();}catch(Exception ignored){}reading.cancel(true);throw new BusinessException(503,"OAuth2令牌自省响应超时");}catch(InterruptedException ex){try{stream.close();}catch(Exception ignored){}reading.cancel(true);Thread.currentThread().interrupt();throw new BusinessException(503,"OAuth2令牌自省被中断");}catch(Exception ex){try{stream.close();}catch(Exception ignored){}throw new BusinessException(503,"OAuth2令牌自省服务暂不可用");}
    }

    private int remainingMillis(long deadline){long remaining=deadline-System.nanoTime();if(remaining<=0)throw new BusinessException(503,"OAuth2令牌自省响应超时");return Math.max(1,(int)Math.min(5000,TimeUnit.NANOSECONDS.toMillis(remaining)));}

    @Transactional
    public Map<String,Object> retry(long deadLetterId,String note,boolean useCurrentMapping){
        Long integrationId=jdbc.sql("SELECT integration_id FROM integration_dead_letter WHERE id=:id").param("id",deadLetterId).query(Long.class).optional().orElseThrow(()->BusinessException.notFound("死信不存在"));
        jdbc.sql("SELECT id FROM integration_config WHERE id=:id FOR UPDATE").param("id",integrationId).query(Long.class).single();
        DeadLetterClaim claim=jdbc.sql("SELECT id,job_id,status FROM integration_dead_letter WHERE id=:id FOR UPDATE").param("id",deadLetterId).query(DeadLetterClaim.class).optional().orElseThrow(()->BusinessException.notFound("死信不存在"));
        if(!Set.of("PENDING","MANUAL_REVIEW").contains(claim.status()))throw new BusinessException(409,"死信正在处理或已解决");
        String jobStatus=jdbc.sql("SELECT status FROM integration_job WHERE id=:id").param("id",claim.jobId()).query(String.class).single();
        if("PROCESSING".equals(jobStatus))throw new BusinessException(409,"关联作业仍由有效或待回收租约处理，请稍后重试");
        if(useCurrentMapping)rebaseJob(claim.jobId());
        jdbc.sql("UPDATE integration_job SET status='FAILED',processing_token=NULL,processing_started_time=NULL,updated_time=now() WHERE id=:id AND status IN ('FAILED','MANUAL_REVIEW','RECEIVED')").param("id",claim.jobId()).update();
        jdbc.sql("UPDATE integration_dead_letter SET status='PENDING',next_retry_time=now(),processing_token=NULL,processing_started_time=NULL,resolution_note=:note,version=version+1,updated_time=now() WHERE id=:id").param("note",note.trim()).param("id",deadLetterId).update();
        audit.record("INTEGRATION_RETRY_QUEUED","INTEGRATION","人工重放集成死信已排队",Map.of("deadLetterId",deadLetterId,"jobId",claim.jobId(),"useCurrentMapping",useCurrentMapping,"note",note.trim()));
        return Map.of("status","PENDING","retryCount",jdbc.sql("SELECT retry_count FROM integration_dead_letter WHERE id=:id").param("id",deadLetterId).query(Integer.class).single(),"jobId",claim.jobId());
    }

    private void rebaseJob(long jobId){
        Map<String,Object> job=jdbc.sql("SELECT integration_id,local_committed,processing_config::text AS processing_config FROM integration_job WHERE id=:id FOR UPDATE").param("id",jobId).query((rs,n)->{Map<String,Object> value=new LinkedHashMap<>();value.put("integrationId",rs.getLong("integration_id"));value.put("localCommitted",rs.getBoolean("local_committed"));value.put("processingConfig",rs.getString("processing_config"));return value;}).single();long integrationId=(Long)job.get("integrationId");ConfigRebase config=jdbc.sql("SELECT config::text AS config,mapping_version,secret_ciphertext,auth_type FROM integration_config WHERE id=:id FOR UPDATE").param("id",integrationId).query(ConfigRebase.class).single();
        if(Boolean.TRUE.equals(job.get("localCommitted"))){Map<String,Object> original=json.map(String.valueOf(job.get("processingConfig"))),current=json.map(config.config());if(!"HTTP".equals(stringOption(current,"deliveryMode","NONE").toUpperCase(Locale.ROOT))||!"HMAC".equals(config.authType())||config.secretCiphertext()==null)throw new BusinessException(409,"已落地事件只能刷新为带独立HMAC凭据的有效HTTP交付配置，不能取消远端交付");for(String key:List.of("deliveryMode","deliveryUrl","deliveryTimeoutMillis","maxRetries","retryDelaysSeconds")){if(current.containsKey(key))original.put(key,current.get(key));else original.remove(key);}validatedOptions(original);jdbc.sql("UPDATE integration_job SET processing_config=CAST(:config AS jsonb),delivery_secret_ciphertext=:secret,status='FAILED',updated_time=now() WHERE id=:id").param("config",json.write(original)).param("secret",config.secretCiphertext()).param("id",jobId).update();return;}
        String deliveryMode=stringOption(json.map(config.config()),"deliveryMode","NONE");String deliverySecret="HTTP".equalsIgnoreCase(deliveryMode)?config.secretCiphertext():null;jdbc.sql("UPDATE integration_job SET mapping_snapshot=CAST(:mappings AS jsonb),processing_config=CAST(:config AS jsonb),delivery_secret_ciphertext=:secret,mapping_version=:version,status='FAILED',updated_time=now() WHERE id=:id").param("mappings",json.write(mappingRows(integrationId))).param("config",config.config()).param("secret",deliverySecret).param("version",config.mappingVersion()).param("id",jobId).update();
    }

    public Map<String,Object> reconcile(long integrationId){
        get(integrationId);long matched=0,superseded=0,missing=0,mismatch=0;
        List<ReconcileRow> rows=jdbc.sql("""
                SELECT j.id,j.mapped_payload::text AS mapped_payload,r.id AS target_id,r.job_id AS target_job_id,r.payload_digest,
                       r.mapped_payload::text AS target_payload
                FROM integration_job j LEFT JOIN integration_processed_record r
                  ON r.trust_domain=j.trust_domain AND r.entity_type=j.entity_type AND r.record_key=j.record_key
                WHERE j.integration_id=:id AND j.local_committed=TRUE ORDER BY j.id
                """).param("id",integrationId).query((rs,n)->new ReconcileRow(rs.getLong("id"),rs.getString("mapped_payload"),rs.getObject("target_id",Long.class),rs.getObject("target_job_id",Long.class),rs.getString("payload_digest"),rs.getString("target_payload"))).list();
        for(ReconcileRow row:rows){if(row.targetId()==null){missing++;continue;}if(row.jobId()!=row.targetJobId()){superseded++;continue;}String expected=sha256(json.canonical(json.map(row.mappedPayload()))),actual=sha256(json.canonical(json.map(row.targetPayload())));if(expected.equals(actual)&&actual.equals(row.payloadDigest()))matched++;else mismatch++;}
        long deliveryPending=jdbc.sql("SELECT count(*) FROM integration_job WHERE integration_id=:id AND local_committed=TRUE AND delivery_status='PENDING'").param("id",integrationId).query(Long.class).single();
        Map<String,Long> counts=Map.of("matched",matched,"superseded",superseded,"missing",missing,"digestMismatch",mismatch,"deliveryPending",deliveryPending);
        audit.record("RECONCILE","INTEGRATION","执行集成落地对账",Map.of("integrationId",integrationId,"result",counts));return new LinkedHashMap<>(counts);
    }

    @Scheduled(initialDelayString="${rdp.integration.retry-initial-delay-ms:30000}",fixedDelayString="${rdp.integration.retry-delay-ms:30000}")
    public void retryDue(){
        transactions.executeWithoutResult(status->jdbc.sql("UPDATE integration_dead_letter SET status='PENDING',processing_token=NULL,processing_started_time=NULL,next_retry_time=now(),error_message='处理租约超时，已自动恢复',version=version+1,updated_time=now() WHERE status='RETRYING' AND processing_started_time<now()-interval '5 minutes'").update());
        transactions.executeWithoutResult(status->jdbc.sql("""
                UPDATE integration_job j SET status=CASE WHEN EXISTS(
                    SELECT 1 FROM integration_dead_letter d WHERE d.job_id=j.id AND d.status IN ('PENDING','RETRYING','MANUAL_REVIEW')
                ) THEN 'FAILED' ELSE 'RECEIVED' END,processing_token=NULL,processing_started_time=NULL,updated_time=now()
                WHERE j.status='PROCESSING' AND j.processing_started_time<now()-interval '5 minutes'
                """).update());
        for(int i=0;i<50;i++){UUID token=UUID.randomUUID();Long id=transactions.execute(status->claimReceived(token));if(id==null)break;processSafely(id,null,token,"system");}
        for(int i=0;i<50;i++){
            UUID token=UUID.randomUUID();DeadLetterClaim claim=transactions.execute(status->claimDue(token));if(claim==null)break;processSafely(claim.jobId(),claim.id(),token,"system");
        }
    }

    private Receipt persistReceipt(ConfigAuth auth,String key,JsonNode source,String digest,String clientIp){
        ExistingJob existing=jdbc.sql("SELECT id,payload_digest,status FROM integration_job WHERE system_code=:code AND idempotency_key=:key").param("code",auth.systemCode()).param("key",key).query(ExistingJob.class).optional().orElse(null);
        if(existing!=null){if(!digest.equals(existing.payloadDigest()))throw new BusinessException(409,"幂等键已用于不同载荷");return new Receipt(existing.id(),existing.status(),false);}
        ConfigReceipt locked=jdbc.sql("SELECT system_code,active,config::text AS config,mapping_version,version FROM integration_config WHERE id=:id FOR UPDATE").param("id",auth.id()).query(ConfigReceipt.class).single();
        if(!locked.active()||locked.version()!=auth.version()||locked.mappingVersion()!=auth.mappingVersion())throw new BusinessException(409,"集成配置或映射刚刚变更，请使用最新配置重新发送");
        List<Map<String,Object>> mappings=mappingRows(auth.id());String config=locked.config();
        Long jobId=jdbc.sql("""
                INSERT INTO integration_job(integration_id,system_code,direction,status,idempotency_key,record_count,payload_digest,source_payload,
                                            mapping_snapshot,processing_config,delivery_secret_ciphertext,mapping_version,initial_mapping_snapshot,initial_processing_config,initial_mapping_version)
                VALUES (:integration,:code,'INBOUND','RECEIVED',:key,1,:digest,CAST(:source AS jsonb),CAST(:mappings AS jsonb),CAST(:config AS jsonb),:secret,:mappingVersion,
                        CAST(:mappings AS jsonb),CAST(:config AS jsonb),:mappingVersion)
                ON CONFLICT (system_code,idempotency_key) DO NOTHING RETURNING id
                """).param("integration",auth.id()).param("code",auth.systemCode()).param("key",key).param("digest",digest).param("source",json.write(source)).param("mappings",json.write(mappings)).param("config",config).param("secret","HTTP".equalsIgnoreCase(stringOption(json.map(config),"deliveryMode","NONE"))?auth.secretCiphertext():null).param("mappingVersion",locked.mappingVersion()).query(Long.class).optional().orElse(null);
        if(jobId==null){ExistingJob winner=jdbc.sql("SELECT id,payload_digest,status FROM integration_job WHERE system_code=:code AND idempotency_key=:key").param("code",auth.systemCode()).param("key",key).query(ExistingJob.class).single();if(!digest.equals(winner.payloadDigest()))throw new BusinessException(409,"幂等键已用于不同载荷");return new Receipt(winner.id(),winner.status(),false);}
        audit.recordAs(null,"integration:"+auth.systemCode(),"WEBHOOK_RECEIVED","INTEGRATION","外部事件认证后持久接收",Map.of("jobId",jobId,"systemCode",auth.systemCode(),"authType",auth.authType(),"payloadDigest",digest,"mappingVersion",locked.mappingVersion(),"clientIp",clientIp));return new Receipt(jobId,"RECEIVED",true);
    }

    private void processSafely(long jobId,Long deadLetterId,UUID token,String actor){
        try{
            Prepared prepared=prepare(jobId,token);
            LocalOutcome outcome=transactions.execute(status->finalizeLocal(prepared,deadLetterId,token,actor));
            if(outcome==null||!outcome.deliveryRequired())return;
            deliverIfConfigured(prepared);
            transactions.executeWithoutResult(status->completeDelivery(prepared,deadLetterId,token,actor,outcome));
        }
        catch(ProcessingException ex){transactions.executeWithoutResult(status->markProcessingFailure(jobId,deadLetterId,token,ex,actor));}
        catch(Exception ex){transactions.executeWithoutResult(status->markUnexpected(jobId,deadLetterId,token,ex,actor));}
    }

    private void markProcessingFailure(long jobId,Long deadLetterId,UUID token,ProcessingException error,String actor){
        Job job=jobForUpdate(jobId,token);if(job==null)return;failJob(job,deadLetterId,token,error,json.map(job.processingConfig()),actor);
    }

    private Prepared prepare(long jobId,UUID token){
        Job job=jdbc.sql("SELECT id,integration_id,system_code,idempotency_key,status,source_payload::text AS source_payload,mapping_snapshot::text AS mapping_snapshot,processing_config::text AS processing_config,mapped_payload::text AS mapped_payload,mapping_version,attempt_count,processing_token,local_committed,delivery_status,cursor_value,delivery_secret_ciphertext FROM integration_job WHERE id=:id AND status='PROCESSING' AND processing_token=:token").param("id",jobId).param("token",token).query(Job.class).optional().orElseThrow(()->new ProcessingException("LEASE_LOST","集成作业处理租约已失效"));
        JsonNode source=parseStoredObject(job.sourcePayload());Map<String,Object> options=json.map(job.processingConfig());
        if(job.localCommitted()){
            if(job.mappedPayload()==null||!"PENDING".equals(job.deliveryStatus()))throw new ProcessingException("DELIVERY_STATE","已落地作业的交付状态不正确");
            JsonNode mapped=parseStoredObject(job.mappedPayload());String mappedJson=json.canonical(mapped);return new Prepared(job,source,mapped,mappedJson,sha256(mappedJson),options,job.cursorValue());
        }
        String cursor=extractOptional(source,stringOption(options,"cursorField",null));if(cursor!=null)validateCursor(cursor,stringOption(options,"cursorType","LEXICOGRAPHIC"));int fenced=jdbc.sql("UPDATE integration_job SET cursor_value=:cursor,updated_time=now() WHERE id=:id AND status='PROCESSING' AND processing_token=:token").param("cursor",cursor).param("id",job.id()).param("token",token).update();if(fenced!=1)throw new ProcessingException("LEASE_LOST","集成作业处理租约已失效");JsonNode mapped=applyMappings(source,job.mappingSnapshot());String mappedJson=json.canonical(mapped);return new Prepared(job,source,mapped,mappedJson,sha256(mappedJson),options,cursor);
    }

    private void deliverIfConfigured(Prepared prepared){String mode=stringOption(prepared.options(),"deliveryMode","NONE").toUpperCase(Locale.ROOT);if("NONE".equals(mode))return;if(!"HTTP".equals(mode))throw new ProcessingException("DELIVERY_CONFIG","交付适配器类型不正确");String endpoint=stringOption(prepared.options(),"deliveryUrl",null);if(endpoint==null)throw new ProcessingException("DELIVERY_CONFIG","HTTP交付地址未配置");URI deliveryUri;try{deliveryUri=validateDeliveryEndpoint(endpoint);}catch(BusinessException ex){throw new ProcessingException("DELIVERY_CONFIG",ex.getMessage());}int timeout=intOption(prepared.options(),"deliveryTimeoutMillis",5000,100,30000);String timestamp=Long.toString(Instant.now().getEpochSecond());HttpRequest.Builder builder=HttpRequest.newBuilder(deliveryUri).timeout(Duration.ofMillis(timeout)).header("Content-Type","application/json").header("X-Source-System",prepared.job().systemCode()).header("X-Integration-Job-Id",Long.toString(prepared.job().id())).header("X-Idempotency-Key",prepared.job().idempotencyKey()).header("X-Signature-Version","RDP-HMAC-V1");String encrypted=prepared.job().deliverySecretCiphertext();if(encrypted==null)throw new ProcessingException("CREDENTIAL_ROTATION_REQUIRED","接收时交付凭据快照不可用，请轮换并人工rebase");String deliverySecret;try{deliverySecret=requireStrongHmacSecret(encrypted);}catch(RuntimeException ex){throw new ProcessingException("CREDENTIAL_ROTATION_REQUIRED","交付凭据不符合当前强度策略，请轮换并人工rebase");}String target=deliveryUri.getRawPath()+(deliveryUri.getRawQuery()==null?"":"?"+deliveryUri.getRawQuery()),signingInput="RDP-HMAC-V1\nPOST\n"+target+"\n"+prepared.job().systemCode()+"\n"+timestamp+"\n"+prepared.job().idempotencyKey()+"\n"+prepared.mappedJson(),signature=hmac(deliverySecret,signingInput);builder.header("X-Timestamp",timestamp).header("X-Signature",signature);try{HttpResponse<Void> response=http.send(builder.POST(HttpRequest.BodyPublishers.ofString(prepared.mappedJson(),StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.discarding());int code=response.statusCode();if(code>=200&&code<300)return;Integer retryAfter=parseRetryAfter(response.headers().firstValue("Retry-After").orElse(null));if(code==408||code==425||code==429||code>=500)throw new ProcessingException("REMOTE_RETRYABLE","外部交付端点暂时不可用（HTTP "+code+"）",retryAfter);throw new ProcessingException("REMOTE_REJECTED","外部交付端点拒绝规范化事件（HTTP "+code+"）");}catch(ProcessingException ex){throw ex;}catch(java.net.http.HttpTimeoutException ex){throw new ProcessingException("REMOTE_TIMEOUT","外部交付端点响应超时");}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new ProcessingException("REMOTE_INTERRUPTED","外部交付被中断");}catch(Exception ex){throw new ProcessingException("REMOTE_UNAVAILABLE","外部交付端点连接失败");}}
    private Integer parseRetryAfter(String raw){if(raw==null)return null;try{int seconds=Integer.parseInt(raw.trim());return seconds>=1&&seconds<=86400?seconds:null;}catch(Exception ignored){}try{long seconds=ChronoUnit.SECONDS.between(Instant.now(),ZonedDateTime.parse(raw.trim(),DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());return seconds>=1&&seconds<=86400?(int)seconds:null;}catch(Exception ignored){return null;}}

    private LocalOutcome finalizeLocal(Prepared prepared,Long deadLetterId,UUID token,String actor){
        Job job=jobForUpdate(prepared.job().id(),token);if(job==null)throw new ProcessingException("LEASE_LOST","集成作业处理租约已失效");
        if(job.localCommitted())return new LocalOutcome(true,jdbc.sql("SELECT record_key FROM integration_job WHERE id=:id").param("id",job.id()).query(String.class).optional().orElse(job.idempotencyKey()),"规范化事件已在本地事务中落地，继续幂等交付");
        ConfigState config=jdbc.sql("SELECT id,active,sync_cursor FROM integration_config WHERE id=:id FOR UPDATE").param("id",job.integrationId()).query(ConfigState.class).single();
        if(!config.active())throw new ProcessingException("CONFIG_DISABLED","集成配置已停用");
        boolean advancesCursor=false;String cursor=prepared.cursor();Map<String,Object> options=prepared.options();
        if(cursor!=null){advancesCursor=config.syncCursor()==null||compareCursor(cursor,config.syncCursor(),stringOption(options,"cursorType","LEXICOGRAPHIC"))>0;if(advancesCursor)ensureNoCursorGap(job,cursor,options,deadLetterId);}
        LandingDecision landing=land(job,prepared.mapped(),prepared.mappedJson(),prepared.mappedDigest(),prepared.source(),options);String nextCursor=advancesCursor?cursor:config.syncCursor();
        if(advancesCursor)jdbc.sql("UPDATE integration_config SET sync_cursor=:cursor,updated_time=now() WHERE id=:id").param("cursor",cursor).param("id",job.integrationId()).update();
        String deliveryMode=stringOption(options,"deliveryMode","NONE").toUpperCase(Locale.ROOT);
        if(!landing.accepted()){
            completeJob(job,prepared.mappedJson(),"SKIPPED",config.syncCursor(),nextCursor,null,"NONE",false);
            if(deadLetterId!=null)resolveDeadLetter(deadLetterId,token,"SKIPPED",actor);
            auditSystem(actor,"INTEGRATION_CONFLICT_SKIPPED",landing.reason(),job,Map.of("recordKey",landing.recordKey(),"mappedDigest",prepared.mappedDigest(),"cursor",cursor==null?"":cursor,"mappingVersion",job.mappingVersion()));
            return new LocalOutcome(false,landing.recordKey(),landing.reason());
        }
        if("NONE".equals(deliveryMode)){
            completeJob(job,prepared.mappedJson(),"COMPLETED",config.syncCursor(),nextCursor,null,"NONE",true);
            if(deadLetterId!=null)resolveDeadLetter(deadLetterId,token,"RESOLVED",actor);
            auditSystem(actor,"INTEGRATION_COMPLETED",landing.reason(),job,Map.of("recordKey",landing.recordKey(),"mappedDigest",prepared.mappedDigest(),"cursor",cursor==null?"":cursor,"mappingVersion",job.mappingVersion()));
            return new LocalOutcome(false,landing.recordKey(),landing.reason());
        }
        int changed=jdbc.sql("UPDATE integration_job SET mapped_payload=CAST(:mapped AS jsonb),cursor_before=:before,cursor_after=:after,local_committed=TRUE,delivery_status='PENDING',error_message=NULL,error_category=NULL,updated_time=now() WHERE id=:id AND status='PROCESSING' AND processing_token=:token")
                .param("mapped",prepared.mappedJson()).param("before",config.syncCursor()).param("after",nextCursor).param("id",job.id()).param("token",token).update();
        if(changed!=1)throw new ProcessingException("LEASE_LOST","集成作业处理租约已失效");
        auditSystem(actor,"INTEGRATION_LANDED","规范化事件已通过信任源裁决并落地，等待远端交付",job,Map.of("recordKey",landing.recordKey(),"mappedDigest",prepared.mappedDigest(),"cursor",cursor==null?"":cursor,"mappingVersion",job.mappingVersion()));
        return new LocalOutcome(true,landing.recordKey(),landing.reason());
    }

    private void completeDelivery(Prepared prepared,Long deadLetterId,UUID token,String actor,LocalOutcome outcome){
        Job job=jobForUpdate(prepared.job().id(),token);if(job==null||!job.localCommitted()||!"PENDING".equals(job.deliveryStatus()))throw new ProcessingException("LEASE_LOST","远端交付作业租约已失效");
        String credentialSnapshotDigest=sha256(job.deliverySecretCiphertext());int changed=jdbc.sql("UPDATE integration_job SET status='COMPLETED',delivery_status='COMPLETED',delivery_secret_ciphertext=NULL,error_message=NULL,error_category=NULL,processing_token=NULL,processing_started_time=NULL,finished_time=now(),updated_time=now() WHERE id=:id AND status='PROCESSING' AND processing_token=:token AND local_committed=TRUE AND delivery_status='PENDING'").param("id",job.id()).param("token",token).update();
        if(changed!=1)throw new ProcessingException("LEASE_LOST","远端交付作业租约已失效");
        if(deadLetterId!=null)resolveDeadLetter(deadLetterId,token,"RESOLVED",actor);
        auditSystem(actor,"INTEGRATION_DELIVERED","本地已接受事件完成幂等远端交付",job,Map.of("recordKey",outcome.recordKey(),"mappedDigest",prepared.mappedDigest(),"mappingVersion",job.mappingVersion(),"credentialSnapshotDigest",credentialSnapshotDigest));
    }

    private LandingDecision land(Job job,JsonNode mapped,String mappedJson,String digest,JsonNode source,Map<String,Object> options){
        String recordKey=extractOptional(source,stringOption(options,"recordKeyField",null));if(recordKey==null)recordKey=job.systemCode()+":"+job.idempotencyKey();if(recordKey.length()>240)throw new ProcessingException("RECORD_KEY","业务主键最长240字符");
        String trustDomain=stringOption(options,"trustDomain",job.systemCode()).trim().toUpperCase(Locale.ROOT),entityType=stringOption(options,"entityType","GENERIC").trim().toUpperCase(Locale.ROOT);if(!trustDomain.matches("^[A-Z][A-Z0-9_-]{0,79}$")||!entityType.matches("^[A-Z][A-Z0-9_-]{0,79}$"))throw new ProcessingException("CONFIG","信任域或实体类型格式不正确");
        String version=extractOptional(source,stringOption(options,"sourceVersionField",null));int priority=intOption(options,"sourcePriority",0,0,10000);String incomingPolicy=stringOption(options,"conflictPolicy","PRIORITY_THEN_VERSION").toUpperCase();if(!CONFLICT_POLICIES.contains(incomingPolicy))throw new ProcessingException("CONFIG","冲突策略不正确");
        String lockKey=trustDomain+"|"+entityType+"|"+recordKey;jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended(:key,31))) integration_record_lock").param("key",lockKey).query(Long.class).single();
        jdbc.sql("UPDATE integration_job SET trust_domain=:domain,entity_type=:entity,record_key=:key WHERE id=:id").param("domain",trustDomain).param("entity",entityType).param("key",recordKey).param("id",job.id()).update();
        Processed current=jdbc.sql("SELECT id,source_system_code,source_priority,source_version,conflict_policy,payload_digest,job_id FROM integration_processed_record WHERE trust_domain=:domain AND entity_type=:entity AND record_key=:key FOR UPDATE").param("domain",trustDomain).param("entity",entityType).param("key",recordKey).query(Processed.class).optional().orElse(null);
        if(current==null){ensureNoDeliveryGap(job,trustDomain,entityType,recordKey);long id=jdbc.sql("INSERT INTO integration_processed_record(trust_domain,entity_type,record_key,integration_id,source_system_code,source_priority,source_version,conflict_policy,mapped_payload,payload_digest,job_id) VALUES (:domain,:entity,:key,:integration,:code,:priority,:version,:policy,CAST(:payload AS jsonb),:digest,:job) RETURNING id").param("domain",trustDomain).param("entity",entityType).param("key",recordKey).param("integration",job.integrationId()).param("code",job.systemCode()).param("priority",priority).param("version",version).param("policy",incomingPolicy).param("payload",mappedJson).param("digest",digest).param("job",job.id()).query(Long.class).single();refreshLandingDigest(id);return new LandingDecision(true,recordKey,"标准化事件已落地");}
        boolean accepted=acceptIncoming(current,priority,version,job.systemCode());String reason=accepted?"较高信任源或较新版本覆盖规范化记录":"较低信任源或旧版本事件未覆盖权威记录";
        if(accepted){ensureNoDeliveryGap(job,trustDomain,entityType,recordKey);jdbc.sql("UPDATE integration_processed_record SET integration_id=:integration,source_system_code=:code,source_priority=:priority,source_version=:version,mapped_payload=CAST(:payload AS jsonb),payload_digest=:digest,job_id=:job,updated_time=now() WHERE id=:id").param("integration",job.integrationId()).param("code",job.systemCode()).param("priority",priority).param("version",version).param("payload",mappedJson).param("digest",digest).param("job",job.id()).param("id",current.id()).update();refreshLandingDigest(current.id());}
        jdbc.sql("INSERT INTO integration_conflict_log(integration_id,job_id,trust_domain,entity_type,record_key,winning_job_id,decision,reason) VALUES (:integration,:job,:domain,:entity,:key,:winner,:decision,:reason)").param("integration",job.integrationId()).param("job",job.id()).param("domain",trustDomain).param("entity",entityType).param("key",recordKey).param("winner",accepted?job.id():current.jobId()).param("decision",accepted?"ACCEPTED":"SKIPPED").param("reason",reason).update();
        return new LandingDecision(accepted,recordKey,reason);
    }

    private boolean acceptIncoming(Processed current,int priority,String version,String systemCode){String policy=current.conflictPolicy();
        if("VERSION_ONLY".equals(policy))return compareVersions(version,current.sourceVersion())>0;
        if(priority!=current.sourcePriority())return priority>current.sourcePriority();
        if("PRIORITY_ONLY".equals(policy))return false;
        int compared=compareVersions(version,current.sourceVersion());if(compared!=0)return compared>0;if(version==null&&systemCode.equals(current.sourceSystemCode()))return true;return systemCode.compareTo(current.sourceSystemCode())<0;
    }
    private void ensureNoDeliveryGap(Job job,String trustDomain,String entityType,String recordKey){long pending=jdbc.sql("SELECT count(*) FROM integration_job WHERE id<>:job AND trust_domain=:domain AND entity_type=:entity AND record_key=:key AND local_committed=TRUE AND delivery_status='PENDING'").param("job",job.id()).param("domain",trustDomain).param("entity",entityType).param("key",recordKey).query(Long.class).single();if(pending>0)throw new ProcessingException("DELIVERY_GAP","同一业务实体存在更早的未完成远端交付");}
    private void refreshLandingDigest(long id){String stored=jdbc.sql("SELECT mapped_payload::text FROM integration_processed_record WHERE id=:id").param("id",id).query(String.class).single();String digest=sha256(json.canonical(json.map(stored)));jdbc.sql("UPDATE integration_processed_record SET payload_digest=:digest WHERE id=:id").param("digest",digest).param("id",id).update();}

    private void completeJob(Job job,String mappedJson,String state,String before,String after,String error,String deliveryStatus,boolean localCommitted){
        jdbc.sql("UPDATE integration_job SET status=:status,mapped_payload=CAST(:mapped AS jsonb),cursor_before=:before,cursor_after=:after,error_message=:error,error_category=NULL,local_committed=:committed,delivery_status=:delivery,delivery_secret_ciphertext=NULL,processing_token=NULL,processing_started_time=NULL,finished_time=now(),updated_time=now() WHERE id=:id")
                .param("status",state).param("mapped",mappedJson).param("before",before).param("after",after).param("error",error).param("committed",localCommitted).param("delivery",deliveryStatus).param("id",job.id()).update();
    }

    private void failJob(Job job,Long deadLetterId,UUID token,ProcessingException ex,Map<String,Object> options,String actor){
        String message=safeError(ex.getMessage());jdbc.sql("UPDATE integration_job SET status='FAILED',error_category=:category,error_message=:error,processing_token=NULL,processing_started_time=NULL,finished_time=now(),updated_time=now() WHERE id=:id").param("category",ex.category).param("error",message).param("id",job.id()).update();
        if(deadLetterId==null){int delay=ex.retryAfterSeconds==null?retryDelays(options).get(0):ex.retryAfterSeconds;jdbc.sql("""
                INSERT INTO integration_dead_letter(integration_id,job_id,event_key,payload,error_message,retry_count,status,next_retry_time)
                VALUES (:integration,:job,:key,(SELECT source_payload FROM integration_job WHERE id=:job),:error,0,'PENDING',now()+make_interval(secs=>:delay))
                ON CONFLICT (integration_id,event_key) DO UPDATE SET job_id=EXCLUDED.job_id,payload=EXCLUDED.payload,error_message=EXCLUDED.error_message,status='PENDING',next_retry_time=EXCLUDED.next_retry_time,updated_time=now()
                """).param("integration",job.integrationId()).param("job",job.id()).param("key",job.idempotencyKey()).param("error",message).param("delay",delay).update();
        }else{
            DeadLetterState state=jdbc.sql("SELECT retry_count,status FROM integration_dead_letter WHERE id=:id AND status='RETRYING' AND processing_token=:token FOR UPDATE").param("id",deadLetterId).param("token",token).query(DeadLetterState.class).optional().orElseThrow(()->new BusinessException(409,"死信处理租约已失效"));
            int attempt=state.retryCount()+1,max=intOption(options,"maxRetries",3,1,10);boolean exhausted=attempt>=max;List<Integer> delays=retryDelays(options);int delay=ex.retryAfterSeconds==null?delays.get(Math.min(attempt,delays.size()-1)):ex.retryAfterSeconds;String next=exhausted?"MANUAL_REVIEW":"PENDING";
            jdbc.sql("UPDATE integration_dead_letter SET retry_count=:attempt,status=:status,error_message=:error,next_retry_time=CASE WHEN :manual THEN NULL ELSE now()+make_interval(secs=>:delay) END,processing_token=NULL,processing_started_time=NULL,last_attempt_time=now(),version=version+1,updated_time=now() WHERE id=:id AND processing_token=:token")
                    .param("attempt",attempt).param("status",next).param("error",message).param("manual",exhausted).param("delay",delay).param("id",deadLetterId).param("token",token).update();
            insertAttempt(deadLetterId,attempt,"FAILED",ex.category,message,token);
            if(exhausted)jdbc.sql("UPDATE integration_job SET status='MANUAL_REVIEW',updated_time=now() WHERE id=:id").param("id",job.id()).update();
        }
        auditSystem(actor,"INTEGRATION_FAILED","外部事件处理失败并进入持久死信",job,Map.of("errorCategory",ex.category,"errorMessage",message,"mappingVersion",job.mappingVersion(),"mappingSnapshotDigest",canonicalDigest(job.mappingSnapshot()),"processingConfigDigest",canonicalDigest(job.processingConfig())));
    }

    private void markUnexpected(long jobId,Long deadLetterId,UUID token,Exception error,String actor){
        Job job=jobForUpdate(jobId,token);if(job==null)return;
        failJob(job,deadLetterId,token,new ProcessingException("INTERNAL","内部处理异常，请由管理员检查配置或死信详情"),json.map(job.processingConfig()),actor);
    }

    private void resolveDeadLetter(long id,UUID token,String result,String actor){
        DeadLetterState state=jdbc.sql("SELECT retry_count,status FROM integration_dead_letter WHERE id=:id FOR UPDATE").param("id",id).query(DeadLetterState.class).optional().orElse(null);if(state==null||"RESOLVED".equals(state.status()))return;
        if(token!=null){int changed=jdbc.sql("UPDATE integration_dead_letter SET status='RESOLVED',resolved_time=now(),resolved_by=:user,resolution_note=:note,next_retry_time=NULL,processing_token=NULL,processing_started_time=NULL,last_attempt_time=now(),version=version+1,updated_time=now() WHERE id=:id AND status='RETRYING' AND processing_token=:token").param("user",currentUserId()).param("note",result).param("id",id).param("token",token).update();if(changed!=1)throw new BusinessException(409,"死信处理租约已失效");insertAttempt(id,state.retryCount()+1,"SKIPPED".equals(result)?"SKIPPED":"RESOLVED",null,null,token);}else jdbc.sql("UPDATE integration_dead_letter SET status='RESOLVED',resolved_time=now(),resolution_note=:note,next_retry_time=NULL,updated_time=now() WHERE id=:id").param("note",result).param("id",id).update();
    }

    private DeadLetterClaim claimDue(UUID token){
        Long integrationId=jdbc.sql("""
                SELECT c.id FROM integration_config c
                WHERE EXISTS(SELECT 1 FROM integration_dead_letter d JOIN integration_job j ON j.id=d.job_id WHERE d.integration_id=c.id AND d.status='PENDING' AND d.next_retry_time<=now() AND (c.active=TRUE OR j.local_committed=TRUE))
                  AND NOT EXISTS(SELECT 1 FROM integration_job active_job WHERE active_job.integration_id=c.id AND active_job.status='PROCESSING')
                ORDER BY (SELECT min(d.next_retry_time) FROM integration_dead_letter d WHERE d.integration_id=c.id AND d.status='PENDING'),c.id
                FOR UPDATE SKIP LOCKED LIMIT 1
                """).query(Long.class).optional().orElse(null);if(integrationId==null)return null;
        Long id=jdbc.sql("SELECT d.id FROM integration_dead_letter d JOIN integration_job j ON j.id=d.job_id JOIN integration_config c ON c.id=d.integration_id WHERE d.integration_id=:integration AND d.status='PENDING' AND d.next_retry_time<=now() AND (c.active=TRUE OR j.local_committed=TRUE) ORDER BY d.next_retry_time,d.id FOR UPDATE OF d SKIP LOCKED LIMIT 1").param("integration",integrationId).query(Long.class).optional().orElse(null);if(id==null)return null;DeadLetterClaim claim=claimDeadLetter(id,token,true);if(claim==null)return null;if(claimJob(claim.jobId(),token))return claim;String jobState=jdbc.sql("SELECT status FROM integration_job WHERE id=:id").param("id",claim.jobId()).query(String.class).single();if(Set.of("COMPLETED","SKIPPED").contains(jobState)){jdbc.sql("UPDATE integration_dead_letter SET status='RESOLVED',resolved_time=now(),resolution_note=:note,processing_token=NULL,processing_started_time=NULL,next_retry_time=NULL,version=version+1,updated_time=now() WHERE id=:id").param("note",jobState).param("id",id).update();}else{jdbc.sql("UPDATE integration_dead_letter SET status='PENDING',processing_token=NULL,processing_started_time=NULL,version=version+1,updated_time=now() WHERE id=:id").param("id",id).update();}return null;
    }

    private Long claimReceived(UUID token){Long integrationId=jdbc.sql("""
                SELECT c.id FROM integration_config c
                WHERE EXISTS(SELECT 1 FROM integration_job queued WHERE queued.integration_id=c.id AND queued.status='RECEIVED' AND (c.active=TRUE OR queued.local_committed=TRUE))
                  AND NOT EXISTS(SELECT 1 FROM integration_job active_job WHERE active_job.integration_id=c.id AND active_job.status='PROCESSING')
                ORDER BY (SELECT min(queued.id) FROM integration_job queued WHERE queued.integration_id=c.id AND queued.status='RECEIVED'),c.id
                FOR UPDATE SKIP LOCKED LIMIT 1
                """).query(Long.class).optional().orElse(null);if(integrationId==null)return null;Long id=jdbc.sql("SELECT j.id FROM integration_job j JOIN integration_config c ON c.id=j.integration_id WHERE j.integration_id=:integration AND j.status='RECEIVED' AND (c.active=TRUE OR j.local_committed=TRUE) ORDER BY j.id FOR UPDATE OF j SKIP LOCKED LIMIT 1").param("integration",integrationId).query(Long.class).optional().orElse(null);return id!=null&&claimJob(id,token)?id:null;}
    private boolean claimJob(long id,UUID token){return jdbc.sql("UPDATE integration_job SET status='PROCESSING',processing_token=:token,processing_started_time=now(),attempt_count=attempt_count+1,updated_time=now() WHERE id=:id AND status IN ('RECEIVED','FAILED','MANUAL_REVIEW')").param("token",token).param("id",id).update()==1;}
    private Job jobForUpdate(long id,UUID token){return jdbc.sql("SELECT id,integration_id,system_code,idempotency_key,status,source_payload::text AS source_payload,mapping_snapshot::text AS mapping_snapshot,processing_config::text AS processing_config,mapped_payload::text AS mapped_payload,mapping_version,attempt_count,processing_token,local_committed,delivery_status,cursor_value,delivery_secret_ciphertext FROM integration_job WHERE id=:id AND status='PROCESSING' AND processing_token=:token FOR UPDATE").param("id",id).param("token",token).query(Job.class).optional().orElse(null);}

    private DeadLetterClaim claimDeadLetter(long id,UUID token,boolean automatic){
        DeadLetterClaim claim=jdbc.sql("SELECT id,job_id,status FROM integration_dead_letter WHERE id=:id FOR UPDATE").param("id",id).query(DeadLetterClaim.class).optional().orElseThrow(()->BusinessException.notFound("死信不存在"));
        if("RESOLVED".equals(claim.status())||"RETRYING".equals(claim.status()))return null;
        int changed=jdbc.sql("UPDATE integration_dead_letter SET status='RETRYING',processing_token=:token,processing_started_time=now(),updated_time=now(),version=version+1 WHERE id=:id AND status IN ('PENDING','MANUAL_REVIEW')").param("token",token).param("id",id).update();return changed==1?new DeadLetterClaim(claim.id(),claim.jobId(),"RETRYING"):null;
    }

    private JsonNode applyMappings(JsonNode source,String snapshot){
        List<MappingSnapshot> mappings;try{mappings=json.mapper().readValue(snapshot,new TypeReference<>(){});}catch(Exception ex){throw new ProcessingException("MAPPING_SNAPSHOT","映射快照不可读取");}
        if(mappings.isEmpty())return source.deepCopy();ObjectNode output=json.mapper().createObjectNode();
        for(MappingSnapshot mapping:mappings){JsonNode value=lookup(source,mapping.sourceField());String raw=value==null||value.isNull()?null:value.isValueNode()?value.asText():json.canonical(value);
            if((raw==null||raw.isBlank())&&mapping.defaultValue()!=null)raw=mapping.defaultValue();
            if((raw==null||raw.isBlank())&&mapping.required())throw new ProcessingException("REQUIRED_FIELD","必填源字段缺失: "+mapping.sourceField());
            JsonNode transformed=transform(raw,value,mapping);if(transformed!=null)setPath(output,mapping.targetField(),transformed);
        }return output;
    }

    private JsonNode transform(String raw,JsonNode original,MappingSnapshot mapping){
        if(raw==null)return null;String type=normalizeTransform(mapping.transformation());
        try{return switch(type){
            case "DIRECT" -> original!=null&&!original.isNull()&&mapping.defaultValue()==null?original.deepCopy():json.mapper().valueToTree(raw);
            case "TRIM" -> json.mapper().valueToTree(raw.trim());case "UPPERCASE" -> json.mapper().valueToTree(raw.trim().toUpperCase(Locale.ROOT));case "LOWERCASE" -> json.mapper().valueToTree(raw.trim().toLowerCase(Locale.ROOT));
            case "NUMBER" -> json.mapper().valueToTree(new BigDecimal(raw.trim()).stripTrailingZeros());case "UNIT" -> json.mapper().valueToTree(new BigDecimal(raw.trim()).multiply(new BigDecimal(mapping.defaultValue())).stripTrailingZeros());
            case "DATE" -> json.mapper().valueToTree(normalizeDate(raw));case "CONSTANT" -> json.mapper().valueToTree(mapping.defaultValue());default -> throw new ProcessingException("TRANSFORM","不支持的字段转换: "+type);
        };}catch(NumberFormatException ex){throw new ProcessingException("NUMBER_FORMAT","字段不是有效数值: "+mapping.sourceField());}
    }

    private String normalizeDate(String raw){String value=raw.trim();try{return Instant.parse(value).toString();}catch(Exception ignored){}try{return OffsetDateTime.parse(value).toInstant().toString();}catch(Exception ignored){}try{return LocalDate.parse(value).toString();}catch(Exception ignored){}throw new ProcessingException("DATE_FORMAT","日期格式必须是ISO-8601");}
    private JsonNode lookup(JsonNode root,String path){JsonNode value=root;for(String part:path.split("\\.")){if(value==null||!value.isObject())return null;value=value.get(part);}return value;}
    private void setPath(ObjectNode root,String path,JsonNode value){String[] parts=path.split("\\.");ObjectNode cursor=root;for(int i=0;i<parts.length-1;i++){JsonNode existing=cursor.get(parts[i]);if(existing!=null&&!existing.isObject())throw new ProcessingException("TARGET_PATH","目标字段路径冲突: "+path);if(existing==null)cursor=cursor.putObject(parts[i]);else cursor=(ObjectNode)existing;}cursor.set(parts[parts.length-1],value);}
    private String extractOptional(JsonNode source,String path){if(path==null)return null;JsonNode value=lookup(source,path);if(value==null||value.isNull()||!value.isValueNode()||value.asText().isBlank())throw new ProcessingException("CONFIGURED_FIELD","配置字段缺失: "+path);return value.asText().trim();}

    private void ensureNoCursorGap(Job job,String cursor,Map<String,Object> options,Long ownDeadLetter){
        String type=stringOption(options,"cursorType","LEXICOGRAPHIC").toUpperCase(),comparison=switch(type){case "NUMBER"->"j.cursor_value::numeric < CAST(:cursor AS numeric)";case "INSTANT"->"j.cursor_value::timestamptz < CAST(:cursor AS timestamptz)";case "LEXICOGRAPHIC"->"j.cursor_value < CAST(:cursor AS text)";default->throw new ProcessingException("CONFIG","游标类型不正确");};
        long gaps=jdbc.sql("SELECT count(*) FROM integration_job j JOIN integration_dead_letter d ON d.job_id=j.id WHERE j.integration_id=:integration AND j.id<>:job AND j.cursor_value IS NOT NULL AND d.status IN ('PENDING','RETRYING','MANUAL_REVIEW') AND d.id<>:own AND "+comparison).param("integration",job.integrationId()).param("job",job.id()).param("own",ownDeadLetter==null?-1L:ownDeadLetter).param("cursor",cursor).query(Long.class).single();
        if(gaps>0)throw new ProcessingException("CURSOR_GAP","存在更早失败事件，当前游标不会推进");
    }
    private void validateCursor(String value,String type){if(!CURSOR_TYPES.contains(type.toUpperCase()))throw new ProcessingException("CONFIG","游标类型不正确");compareCursor(value,value,type);}
    private int compareCursor(String left,String right,String type){try{return switch(type.toUpperCase()){case "NUMBER"->new BigDecimal(left).compareTo(new BigDecimal(right));case "INSTANT"->Instant.parse(left).compareTo(Instant.parse(right));case "LEXICOGRAPHIC"->left.compareTo(right);default->throw new ProcessingException("CONFIG","游标类型不正确");};}catch(ProcessingException ex){throw ex;}catch(Exception ex){throw new ProcessingException("CURSOR_FORMAT","游标值与配置类型不匹配");}}
    private int compareVersions(String left,String right){if(left==null&&right==null)return 0;if(left==null)return -1;if(right==null)return 1;try{return new BigDecimal(left).compareTo(new BigDecimal(right));}catch(Exception ignored){return left.compareTo(right);}}

    private Map<String,Object> validatedOptions(Map<String,Object> raw){
        try{Map<String,Object> options=new LinkedHashMap<>(raw==null?Map.of():raw);options.remove("credentialValidationRequired");options.remove("credentialUpgradeState");String cursor=blank(stringValue(options.get("cursorField"))),cursorType=stringOption(options,"cursorType","LEXICOGRAPHIC").toUpperCase();if(cursor!=null&&!CURSOR_TYPES.contains(cursorType))throw BusinessException.badRequest("游标类型不正确");
            intOption(options,"maxRetries",3,1,10);intOption(options,"sourcePriority",0,0,10000);String policy=stringOption(options,"conflictPolicy","PRIORITY_THEN_VERSION").toUpperCase();if(!CONFLICT_POLICIES.contains(policy))throw BusinessException.badRequest("冲突策略不正确");retryDelays(options);
            String deliveryMode=stringOption(options,"deliveryMode","NONE").toUpperCase(Locale.ROOT);if(!Set.of("NONE","HTTP").contains(deliveryMode))throw BusinessException.badRequest("交付适配器类型不正确");String deliveryUrl=blank(stringValue(options.get("deliveryUrl")));if("HTTP".equals(deliveryMode)){if(deliveryUrl==null)throw BusinessException.badRequest("HTTP交付必须配置地址");validateDeliveryEndpoint(deliveryUrl);}intOption(options,"deliveryTimeoutMillis",5000,100,30000);
            Object whitelist=options.get("ipWhitelist");if(whitelist!=null&&(!(whitelist instanceof List<?> list)||list.size()>100||list.stream().anyMatch(v->String.valueOf(v).length()>64)))throw BusinessException.badRequest("IP白名单格式不正确");
            for(String key:List.of("trustDomain","entityType")){String value=blank(stringValue(options.get(key)));if(value!=null&&!value.toUpperCase(Locale.ROOT).matches("^[A-Z][A-Z0-9_-]{0,79}$"))throw BusinessException.badRequest(key+"格式不正确");}
            options.put("cursorType",cursorType);options.put("conflictPolicy",policy);options.put("maxRetries",intOption(options,"maxRetries",3,1,10));options.put("sourcePriority",intOption(options,"sourcePriority",0,0,10000));options.put("deliveryMode",deliveryMode);options.put("deliveryTimeoutMillis",intOption(options,"deliveryTimeoutMillis",5000,100,30000));if(deliveryUrl!=null)options.put("deliveryUrl",deliveryUrl);else options.remove("deliveryUrl");return options;
        }catch(ProcessingException ex){throw BusinessException.badRequest(ex.getMessage());}
    }
    private void validateAuthenticationOptions(String authType,Map<String,Object> options){
        String normalizedAuth=authType.toUpperCase(Locale.ROOT);if(Set.of("HMAC","OAUTH2").contains(normalizedAuth)){Object whitelistRaw=options.get("ipWhitelist");if(!(whitelistRaw instanceof List<?> whitelist)||whitelist.isEmpty())throw BusinessException.badRequest("外部写入接入必须配置至少一个精确IP白名单");LinkedHashSet<String> normalizedIps=new LinkedHashSet<>();for(Object value:whitelist){String ip=canonicalIp(value==null?null:String.valueOf(value));if(ip==null)throw BusinessException.badRequest("IP白名单仅支持精确IPv4或IPv6地址");normalizedIps.add(ip);}options.put("ipWhitelist",List.copyOf(normalizedIps));}
        if(!"OAUTH2".equals(normalizedAuth))return;
        try{
            String endpoint=blank(stringValue(options.get("oauthIntrospectionUrl"))),clientId=blank(stringValue(options.get("oauthClientId")));
            if(endpoint==null)throw BusinessException.badRequest("OAuth2认证必须配置RFC 7662令牌自省地址");
            validateOauthEndpoint(endpoint);
            if(clientId==null||clientId.length()>200||clientId.chars().anyMatch(ch->ch<33||ch>126))throw BusinessException.badRequest("OAuth2客户端标识格式不正确");
            Object scopesRaw=options.get("oauthRequiredScopes");if(!(scopesRaw instanceof List<?> scopes)||scopes.isEmpty()||scopes.size()>20)throw BusinessException.badRequest("OAuth2认证必须配置1至20个必需scope");
            LinkedHashSet<String> normalizedScopes=new LinkedHashSet<>();for(Object scopeValue:scopes){String scope=scopeValue==null?null:blank(String.valueOf(scopeValue));if(scope==null||!scope.matches("^[A-Za-z0-9._:/-]{1,120}$"))throw BusinessException.badRequest("OAuth2必需scope格式不正确");normalizedScopes.add(scope);}
            for(String key:List.of("oauthAudience","oauthIssuer","oauthSourceClientId")){String value=blank(stringValue(options.get(key)));if(value==null)throw BusinessException.badRequest("OAuth2认证必须配置"+key+"以隔离外部系统身份");if(value.length()>300||value.chars().anyMatch(ch->ch<32))throw BusinessException.badRequest(key+"格式不正确");options.put(key,value);}
            options.put("oauthIntrospectionUrl",endpoint);options.put("oauthClientId",clientId);options.put("oauthRequiredScopes",List.copyOf(normalizedScopes));options.put("oauthTimeoutMillis",intOption(options,"oauthTimeoutMillis",3000,100,5000));
        }catch(ProcessingException ex){throw BusinessException.badRequest(ex.getMessage());}
    }
    private void requireOauthClaims(Map<String,Object> options,JsonNode result){
        @SuppressWarnings("unchecked") List<String> required=(List<String>)options.getOrDefault("oauthRequiredScopes",List.of());Set<String> actual=new LinkedHashSet<>();String scopeText=result.path("scope").isTextual()?result.path("scope").textValue():"";for(String value:scopeText.trim().split("\\s+"))if(!value.isBlank())actual.add(value);if(!actual.containsAll(required))throw BusinessException.unauthorized("OAuth2访问令牌缺少必需权限范围");
        String expectedAudience=blank(stringOption(options,"oauthAudience",null));if(expectedAudience!=null){JsonNode aud=result.path("aud");boolean matched=aud.isTextual()&&expectedAudience.equals(aud.textValue());if(aud.isArray())for(JsonNode value:aud)if(value.isTextual()&&expectedAudience.equals(value.textValue()))matched=true;if(!matched)throw BusinessException.unauthorized("OAuth2访问令牌受众不匹配");}
        requireOauthTextClaim(result,"iss",blank(stringOption(options,"oauthIssuer",null)),"OAuth2访问令牌签发方不匹配");
        requireOauthTextClaim(result,"client_id",blank(stringOption(options,"oauthSourceClientId",null)),"OAuth2访问令牌来源客户端不匹配");
    }
    private void requireOauthTextClaim(JsonNode result,String claim,String expected,String message){if(expected!=null&&(!result.path(claim).isTextual()||!expected.equals(result.path(claim).textValue())))throw BusinessException.unauthorized(message);}
    private List<Integer> retryDelays(Map<String,Object> options){Object raw=options.get("retryDelaysSeconds");if(raw==null)return DEFAULT_RETRY_DELAYS;if(!(raw instanceof List<?> list)||list.isEmpty()||list.size()>10)throw new ProcessingException("CONFIG","重试间隔必须是1至10个秒数");List<Integer> values=new ArrayList<>();for(Object value:list){int seconds;try{seconds=Integer.parseInt(String.valueOf(value));}catch(Exception ex){throw new ProcessingException("CONFIG","重试间隔必须是整数秒");}if(seconds<1||seconds>86400)throw new ProcessingException("CONFIG","重试间隔范围为1至86400秒");values.add(seconds);}return List.copyOf(values);}
    private int intOption(Map<String,Object> options,String key,int fallback,int min,int max){Object raw=options.get(key);if(raw==null)return fallback;try{int value=Integer.parseInt(String.valueOf(raw));if(value<min||value>max)throw new Exception();return value;}catch(Exception ex){throw new ProcessingException("CONFIG",key+"配置范围不正确");}}
    private String stringOption(Map<String,Object> options,String key,String fallback){String value=stringValue(options.get(key));return value==null?fallback:value;}
    private String stringValue(Object value){return value==null?null:String.valueOf(value);}

    private JsonNode parseObject(String body){try{JsonNode node=json.mapper().readTree(body);validateJsonShape(node,0,new int[]{0});if(!node.isObject())throw BusinessException.badRequest("Webhook载荷必须是JSON对象");return node;}catch(JsonProcessingException ex){throw BusinessException.badRequest("Webhook载荷必须是有效JSON对象");}}
    private JsonNode parseStoredObject(String body){try{return json.mapper().readTree(body);}catch(Exception ex){throw new ProcessingException("SOURCE_PAYLOAD","已接收载荷不可读取");}}
    private void validateJsonShape(JsonNode node,int depth,int[] count){if(depth>32||++count[0]>20000)throw BusinessException.badRequest("Webhook JSON层级或节点数量超限");if(node.isContainerNode())node.elements().forEachRemaining(child->validateJsonShape(child,depth+1,count));}

    @SuppressWarnings("unchecked") private void validateIp(String configJson,String clientIp){Map<String,Object> config=json.map(configJson);Object value=config.get("ipWhitelist");if(!(value instanceof List<?> list)||list.isEmpty())throw BusinessException.unauthorized("外部系统未配置可信来源IP");String normalized=canonicalIp(clientIp);if(normalized==null||list.stream().map(String::valueOf).noneMatch(normalized::equals))throw BusinessException.unauthorized("来源IP不在白名单");}
    private void validate(ConfigBody body){if(body==null||blank(body.systemCode())==null||!normalizeCode(body.systemCode()).matches("^[A-Z][A-Z0-9_-]{1,49}$"))throw BusinessException.badRequest("系统编码格式不正确");if(blank(body.systemName())==null||body.systemName().length()>100)throw BusinessException.badRequest("系统名称不能为空且最长100字符");if(body.systemType()==null||!TYPES.contains(body.systemType().toUpperCase()))throw BusinessException.badRequest("系统类型不正确");if(body.authType()==null||!AUTH_TYPES.contains(body.authType().toUpperCase()))throw BusinessException.badRequest("认证类型不正确");validateUrl(body.baseUrl());}
    private void requireDeliveryAuthentication(String authType,Map<String,Object> options){if("HTTP".equals(stringOption(options,"deliveryMode","NONE"))&&!"HMAC".equalsIgnoreCase(authType))throw BusinessException.badRequest("HTTP规范载荷交付必须使用HMAC认证配置");}
    private void validateUrl(String value){if(blank(value)==null)return;try{URI uri=URI.create(value);if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();}catch(Exception ex){throw BusinessException.badRequest("服务地址必须是无用户凭据的HTTP(S) URL");}}
    private URI validateDeliveryEndpoint(String value){URI uri;try{uri=URI.create(value);if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null)throw new IllegalArgumentException();}catch(Exception ex){throw BusinessException.badRequest("交付地址必须是无用户凭据和片段的HTTP(S) URL");}if("http".equals(uri.getScheme())&&!allowPrivateDeliveryHosts)throw BusinessException.badRequest("生产交付地址必须使用HTTPS；受控内网HTTP需由部署方显式开启");String host=uri.getHost().toLowerCase(Locale.ROOT);if(!deliveryAllowedHosts.contains(host))throw BusinessException.badRequest("交付地址主机不在部署白名单");try{for(InetAddress address:InetAddress.getAllByName(host))if(!allowPrivateDeliveryHosts&&restrictedDeliveryAddress(address))throw BusinessException.badRequest("交付地址解析到受限私网或保留地址");}catch(BusinessException ex){throw ex;}catch(Exception ex){throw BusinessException.badRequest("交付地址主机无法安全解析");}return uri;}
    private URI validateOauthEndpoint(String value){URI uri=oauthEndpointPolicy(value);try{validateOauthAddresses(InetAddress.getAllByName(uri.getHost()));}catch(BusinessException ex){throw ex;}catch(Exception ex){throw BusinessException.badRequest("OAuth2自省地址主机无法安全解析");}return uri;}
    private URI validateOauthEndpointForRequest(String value,long deadline){URI uri=oauthEndpointPolicy(value);CompletableFuture<InetAddress[]> lookup;try{lookup=CompletableFuture.supplyAsync(()->{try{return InetAddress.getAllByName(uri.getHost());}catch(Exception ex){throw new IllegalStateException(ex);}},oauthDnsExecutor);}catch(RuntimeException ex){throw new BusinessException(503,"OAuth2自省地址解析服务繁忙");}try{validateOauthAddresses(lookup.get(remainingMillis(deadline),TimeUnit.MILLISECONDS));return uri;}catch(TimeoutException ex){lookup.cancel(true);throw new BusinessException(503,"OAuth2自省地址解析超时");}catch(InterruptedException ex){lookup.cancel(true);Thread.currentThread().interrupt();throw new BusinessException(503,"OAuth2自省地址解析被中断");}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new BusinessException(503,"OAuth2自省地址主机无法安全解析");}}
    private URI oauthEndpointPolicy(String value){URI uri;try{uri=URI.create(value);if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getRawQuery()!=null)throw new IllegalArgumentException();}catch(Exception ex){throw BusinessException.badRequest("OAuth2自省地址必须是无用户凭据、查询串和片段的HTTP(S) URL");}if("http".equals(uri.getScheme())&&!allowPrivateOauthHosts)throw BusinessException.badRequest("生产OAuth2自省地址必须使用HTTPS；受控内网HTTP需由部署方显式开启");String host=uri.getHost().toLowerCase(Locale.ROOT);if(!oauthAllowedHosts.contains(host))throw BusinessException.badRequest("OAuth2自省地址主机不在部署白名单");return uri;}
    private void validateOauthAddresses(InetAddress[] addresses){if(addresses==null||addresses.length==0)throw BusinessException.badRequest("OAuth2自省地址主机无法安全解析");for(InetAddress address:addresses)if(!allowPrivateOauthHosts&&restrictedDeliveryAddress(address))throw BusinessException.badRequest("OAuth2自省地址解析到受限私网或保留地址");}
    private Set<String> hostSet(String csv){if(csv==null||csv.isBlank())return Set.of();LinkedHashSet<String> hosts=new LinkedHashSet<>();for(String value:csv.split(",")){String host=value.trim().toLowerCase(Locale.ROOT);if(!host.isBlank()&&host.matches("^[a-z0-9.-]{1,253}$"))hosts.add(host);}return Set.copyOf(hosts);}
    private String canonicalIp(String raw){if(raw==null)return null;String value=raw.trim();if(value.length()<2||value.length()>64||!value.matches("[0-9A-Fa-f:.]+"))return null;if(value.indexOf(':')>=0){try{InetAddress address=InetAddress.getByName(value);return address.getAddress().length==16?address.getHostAddress().toLowerCase(Locale.ROOT):null;}catch(Exception ex){return null;}}String[] octets=value.split("\\.",-1);if(octets.length!=4)return null;StringBuilder normalized=new StringBuilder();for(String octet:octets){try{if(octet.isEmpty()||octet.length()>3)return null;int number=Integer.parseInt(octet);if(number>255)return null;if(!normalized.isEmpty())normalized.append('.');normalized.append(number);}catch(NumberFormatException ex){return null;}}return normalized.toString();}
    public static boolean restrictedDeliveryAddress(InetAddress address){if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress())return true;byte[] bytes=address.getAddress();if(bytes.length==16)return (bytes[0]&0xfe)==0xfc;int first=bytes[0]&0xff,second=bytes[1]&0xff;if(first==0||first>=224)return true;if(first==100&&second>=64&&second<=127)return true;if(first==198&&(second==18||second==19))return true;return first==192&&second==0&&(bytes[2]&0xff)==0;}
    private String secretCiphertext(String authType,String plain,String existing){String normalized=authType.toUpperCase(Locale.ROOT);if(!Set.of("HMAC","OAUTH2").contains(normalized))return blank(plain)==null?existing:cipher.encrypt(plain);if(blank(plain)!=null){validatePlainSecret(normalized,plain);return cipher.encrypt(plain);}if(existing==null)throw BusinessException.badRequest("HMAC或OAuth2配置必须提供共享密钥/客户端密钥");try{validatePlainSecret(normalized,cipher.decrypt(existing));}catch(RuntimeException ex){throw BusinessException.badRequest("现有认证凭据不符合当前策略，请输入新密钥完成轮换");}return existing;}
    private void validatePlainSecret(String authType,String plain){if(blank(plain)==null)throw BusinessException.badRequest("认证密钥不能为空");int bytes=plain.getBytes(StandardCharsets.UTF_8).length;if(plain.chars().anyMatch(Character::isISOControl))throw BusinessException.badRequest("认证密钥不能包含控制字符");if("HMAC".equals(authType)&&(bytes<32||bytes>512))throw BusinessException.badRequest("HMAC共享密钥必须是32至512个UTF-8字节的高强度随机值");if("OAUTH2".equals(authType)&&bytes>4096)throw BusinessException.badRequest("OAuth2自省客户端密钥最长4096个UTF-8字节");}
    private String requireStrongHmacSecret(String encrypted){if(encrypted==null)throw new IllegalStateException("HMAC凭据缺失");String plain=cipher.decrypt(encrypted);validatePlainSecret("HMAC",plain);return plain;}
    private void quarantineInvalidHmacCredential(long integrationId,String systemCode,String observedCiphertext){transactions.executeWithoutResult(status->{String current=jdbc.sql("SELECT secret_ciphertext FROM integration_config WHERE id=:id AND auth_type='HMAC' FOR UPDATE").param("id",integrationId).query(String.class).optional().orElse(null);if(current==null||observedCiphertext==null||!current.equals(observedCiphertext))return;try{requireStrongHmacSecret(current);return;}catch(RuntimeException ignored){}quarantineInvalidHmacCredentialLocked(integrationId,systemCode);});}
    private void quarantineInvalidHmacCredentialLocked(long integrationId,String systemCode){
        jdbc.sql("UPDATE integration_config SET active=FALSE,config=(config-'credentialValidationRequired')||'{\"credentialUpgradeState\":\"ROTATION_REQUIRED\"}'::jsonb,version=version+1,updated_time=now() WHERE id=:id AND auth_type='HMAC'").param("id",integrationId).update();
        jdbc.sql("""
                INSERT INTO integration_dead_letter(integration_id,job_id,event_key,payload,error_message,status,next_retry_time,updated_time)
                SELECT j.integration_id,j.id,coalesce(j.idempotency_key,'credential-rotation-job-'||j.id),j.source_payload,'HMAC凭据不符合当前强度策略，须轮换后人工rebase','MANUAL_REVIEW',NULL,now()
                FROM integration_job j WHERE j.integration_id=:id AND j.delivery_secret_ciphertext IS NOT NULL AND j.status NOT IN ('COMPLETED','SKIPPED')
                ON CONFLICT (integration_id,event_key) DO UPDATE SET status='MANUAL_REVIEW',next_retry_time=NULL,processing_token=NULL,processing_started_time=NULL,error_message=EXCLUDED.error_message,updated_time=now()
                """).param("id",integrationId).update();
        jdbc.sql("UPDATE integration_job SET status='MANUAL_REVIEW',delivery_secret_ciphertext=NULL,error_category='CREDENTIAL_ROTATION_REQUIRED',error_message='HMAC凭据不符合当前强度策略，须轮换后人工rebase',processing_token=NULL,processing_started_time=NULL,updated_time=now() WHERE integration_id=:id AND delivery_secret_ciphertext IS NOT NULL AND status NOT IN ('COMPLETED','SKIPPED')").param("id",integrationId).update();
        audit.recordAs(null,"system","INTEGRATION_CREDENTIAL_ROTATION_REQUIRED","INTEGRATION","遗留HMAC凭据不符合当前强度策略，集成已停用",Map.of("integrationId",integrationId,"systemCode",systemCode));
    }
    private String normalizeTransform(String value){String normalized=value==null?"":value.trim().toUpperCase();if(!TRANSFORMS.contains(normalized))throw BusinessException.badRequest("不支持的字段转换");return normalized;}
    private String normalizeCode(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT);}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    private BigDecimal parseNumber(String value,String label){try{return new BigDecimal(value);}catch(Exception ex){throw BusinessException.badRequest(label+"必须是数值");}}
    private String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private String hmac(String secret,String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private String safeError(String value){String text=value==null?"处理失败":value.replaceAll("[\\r\\n\\t]"," ");return text.substring(0,Math.min(text.length(),900));}
    private Long currentUserId(){try{return CurrentUser.require().id();}catch(Exception ex){return null;}}
    private void auditSystem(String actor,String operation,String description,Job job,Map<String,Object> details){Map<String,Object> safe=new LinkedHashMap<>(details);safe.put("jobId",job.id());safe.put("systemCode",job.systemCode());safe.put("payloadDigest",jdbc.sql("SELECT payload_digest FROM integration_job WHERE id=:id").param("id",job.id()).query(String.class).single());audit.recordAs(null,actor,operation,"INTEGRATION",description,safe);}
    private void insertAttempt(long id,int number,String result,String category,String error,UUID token){AttemptEvidence evidence=jdbc.sql("SELECT j.mapping_version,j.mapping_snapshot::text AS mapping_snapshot,j.processing_config::text AS processing_config FROM integration_job j JOIN integration_dead_letter d ON d.job_id=j.id WHERE d.id=:id").param("id",id).query(AttemptEvidence.class).single();jdbc.sql("INSERT INTO integration_retry_attempt(dead_letter_id,attempt_number,result,error_category,error_message,mapping_version,mapping_snapshot_digest,processing_config_digest,processing_token) VALUES (:id,:number,:result,:category,:error,:mappingVersion,:mappingDigest,:configDigest,:token) ON CONFLICT (dead_letter_id,attempt_number) DO NOTHING").param("id",id).param("number",number).param("result",result).param("category",category).param("error",error).param("mappingVersion",evidence.mappingVersion()).param("mappingDigest",canonicalDigest(evidence.mappingSnapshot())).param("configDigest",canonicalDigest(evidence.processingConfig())).param("token",token).update();}
    private String canonicalDigest(String raw){try{return sha256(json.canonical(json.mapper().readTree(raw)));}catch(Exception ex){throw new IllegalStateException("集成快照摘要计算失败",ex);}}

    private List<Map<String,Object>> mappingRows(long id){return jdbc.sql("SELECT id,source_field,target_field,transformation,required,default_value FROM integration_field_mapping WHERE integration_id=:id ORDER BY id").param("id",id).query((rs,n)->{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("sourceField",rs.getString("source_field"));v.put("targetField",rs.getString("target_field"));v.put("transformation",rs.getString("transformation"));v.put("required",rs.getBoolean("required"));v.put("defaultValue",rs.getString("default_value"));return v;}).list();}
    private Map<String,Object> configRow(ResultSet rs,int n)throws SQLException{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("systemCode",rs.getString("system_code"));v.put("systemName",rs.getString("system_name"));v.put("systemType",rs.getString("system_type"));v.put("baseUrl",rs.getString("base_url"));v.put("authType",rs.getString("auth_type"));v.put("secretConfigured",rs.getString("secret_ciphertext")!=null);v.put("active",rs.getBoolean("active"));v.put("syncCursor",rs.getString("sync_cursor"));v.put("config",json.map(rs.getString("config")));v.put("version",rs.getInt("version"));v.put("mappingVersion",rs.getInt("mapping_version"));v.put("createdTime",rs.getObject("created_time"));v.put("updatedTime",rs.getObject("updated_time"));return v;}
    private Map<String,Object> jobRow(ResultSet rs,int n)throws SQLException{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("integrationId",rs.getObject("integration_id"));v.put("systemCode",rs.getString("system_code"));v.put("direction",rs.getString("direction"));v.put("status",rs.getString("status"));v.put("idempotencyKey",rs.getString("idempotency_key"));v.put("recordCount",rs.getLong("record_count"));v.put("errorMessage",rs.getString("error_message"));v.put("errorCategory",rs.getString("error_category"));v.put("payloadDigest",rs.getString("payload_digest"));v.put("mappingVersion",rs.getInt("mapping_version"));v.put("cursorValue",rs.getString("cursor_value"));v.put("cursorBefore",rs.getString("cursor_before"));v.put("cursorAfter",rs.getString("cursor_after"));v.put("localCommitted",rs.getBoolean("local_committed"));v.put("deliveryStatus",rs.getString("delivery_status"));v.put("attemptCount",rs.getInt("attempt_count"));v.put("startedTime",rs.getObject("started_time"));v.put("finishedTime",rs.getObject("finished_time"));v.put("updatedTime",rs.getObject("updated_time"));return v;}
    private Map<String,Object> deadLetterRow(ResultSet rs,int n)throws SQLException{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("integrationId",rs.getLong("integration_id"));v.put("systemCode",rs.getString("system_code"));v.put("jobId",rs.getLong("job_id"));v.put("eventKey",rs.getString("event_key"));v.put("errorMessage",rs.getString("error_message"));v.put("retryCount",rs.getInt("retry_count"));v.put("status",rs.getString("status"));v.put("localCommitted",rs.getBoolean("local_committed"));v.put("nextRetryTime",rs.getObject("next_retry_time"));v.put("lastAttemptTime",rs.getObject("last_attempt_time"));v.put("createdTime",rs.getObject("created_time"));v.put("resolvedTime",rs.getObject("resolved_time"));v.put("resolvedBy",rs.getObject("resolved_by"));v.put("resolutionNote",rs.getString("resolution_note"));v.put("version",rs.getInt("version"));try{v.put("attempts",json.mapper().readValue(rs.getString("attempts"),new TypeReference<List<Map<String,Object>>>(){}));}catch(Exception ex){v.put("attempts",List.of());}return v;}

    public record ConfigBody(String systemCode,String systemName,String systemType,String baseUrl,String authType,String secret,boolean active,Map<String,Object> config,Integer expectedVersion){}
    public record MappingBody(String sourceField,String targetField,String transformation,boolean required,String defaultValue){}
    public record WebhookResult(long jobId,String status,boolean created,String authType){}
    private record OauthResponse(int statusCode,byte[] body){}
    private record CredentialUpgrade(long id,String systemCode,String secretCiphertext,String config){}
    private record ConfigAuth(long id,String systemCode,String authType,String secretCiphertext,boolean active,String config,int mappingVersion,int version){}
    private record ConfigReceipt(String systemCode,boolean active,String config,int mappingVersion,int version){}
    private record ConfigState(long id,boolean active,String syncCursor){}
    private record ConfigRebase(String config,int mappingVersion,String secretCiphertext,String authType){}
    private record ExistingJob(long id,String payloadDigest,String status){}
    private record Receipt(long jobId,String status,boolean created){}
    private record Job(long id,long integrationId,String systemCode,String idempotencyKey,String status,String sourcePayload,String mappingSnapshot,String processingConfig,String mappedPayload,int mappingVersion,int attemptCount,UUID processingToken,boolean localCommitted,String deliveryStatus,String cursorValue,String deliverySecretCiphertext){}
    private record Prepared(Job job,JsonNode source,JsonNode mapped,String mappedJson,String mappedDigest,Map<String,Object> options,String cursor){}
    private record LocalOutcome(boolean deliveryRequired,String recordKey,String reason){}
    private record MappingSnapshot(Long id,String sourceField,String targetField,String transformation,boolean required,String defaultValue){}
    private record Processed(long id,String sourceSystemCode,int sourcePriority,String sourceVersion,String conflictPolicy,String payloadDigest,long jobId){}
    private record LandingDecision(boolean accepted,String recordKey,String reason){}
    private record DeadLetterState(int retryCount,String status){}
    private record DeadLetterClaim(long id,long jobId,String status){}
    private record ReconcileRow(long jobId,String mappedPayload,Long targetId,Long targetJobId,String payloadDigest,String targetPayload){}
    private record AttemptEvidence(int mappingVersion,String mappingSnapshot,String processingConfig){}
    private static final class ProcessingException extends RuntimeException{private final String category;private final Integer retryAfterSeconds;private ProcessingException(String category,String message){this(category,message,null);}private ProcessingException(String category,String message,Integer retryAfterSeconds){super(message);this.category=category;this.retryAfterSeconds=retryAfterSeconds;}}
}
