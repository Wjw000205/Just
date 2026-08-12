package com.justeam.rdp.device;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.governance.DictionaryOptionService;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.DataScopeService;
import com.justeam.rdp.security.UserPrincipal;
import com.justeam.rdp.security.SecretCipher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceService {
    private final JdbcClient jdbc; private final JsonSupport json; private final DataScopeService scopes; private final AuditService audit;
    private final SecretCipher cipher;private final DictionaryOptionService dictionaries;
    public DeviceService(JdbcClient jdbc, JsonSupport json, DataScopeService scopes, AuditService audit,SecretCipher cipher,DictionaryOptionService dictionaries) {
        this.jdbc=jdbc;this.json=json;this.scopes=scopes;this.audit=audit;this.cipher=cipher;this.dictionaries=dictionaries;
    }

    public List<Map<String,Object>> list(String status, String keyword) {
        UserPrincipal user=CurrentUser.require();
        String scope=user.admin()?"TRUE":user.dataScopes().isEmpty()?"FALSE":"data_scope_id IN (:scopes)";
        JdbcClient.StatementSpec spec=jdbc.sql("""
                SELECT * FROM device WHERE (CAST(:status AS text) IS NULL OR status=CAST(:status AS text))
                AND (CAST(:keyword AS text) IS NULL OR lower(device_code) LIKE lower(concat('%',CAST(:keyword AS text),'%'))
                     OR lower(device_name) LIKE lower(concat('%',CAST(:keyword AS text),'%'))) AND
                """+scope+" ORDER BY device_name LIMIT 500").param("status",blank(status)).param("keyword",blank(keyword));
        if(!user.admin()&&!user.dataScopes().isEmpty()) spec=spec.param("scopes",user.dataScopes());
        return spec.query(this::row).list();
    }

    public Map<String,Object> get(long id) {
        Map<String,Object> device=jdbc.sql("SELECT * FROM device WHERE id=:id").param("id",id).query(this::row).optional()
                .orElseThrow(()->BusinessException.notFound("设备不存在"));
        scopes.requireRead(((Number)device.get("dataScopeId")).longValue()); return device;
    }

    @Transactional
    public DeviceRegistration create(DeviceBody body) {
        scopes.require(body.dataScopeId());
        String protocol=dictionaries.requireEnabled("DEVICE_PROTOCOL",body.protocol(),"设备协议");
        String ingestSecret=newSecret();
        Long id=jdbc.sql("""
                INSERT INTO device(device_code,device_name,device_type,model,protocol,status,connection_config,
                                   connection_config_ciphertext,ingest_secret_ciphertext,data_scope_id,created_by,
                                   last_status_change_time,last_status_reason)
                VALUES (:code,:name,:type,:model,:protocol,'OFFLINE','{}'::jsonb,:configCipher,:secretCipher,
                        :scope,:user,now(),'设备登记，等待首个心跳或测点') RETURNING id
                """).param("code",body.deviceCode()).param("name",body.deviceName()).param("type",body.deviceType())
                .param("model",body.model()).param("protocol",protocol).param("configCipher",cipher.encrypt(json.write(body.connectionConfig())))
                .param("secretCipher",cipher.encrypt(ingestSecret))
                .param("scope",body.dataScopeId()).param("user",CurrentUser.require().id()).query(Long.class).single();
        audit.record("CREATE","DEVICE","新增设备",Map.of("deviceId",id,"deviceCode",body.deviceCode(),"protocol",protocol));return new DeviceRegistration(id,ingestSecret);
    }

    @Transactional
    public void update(long id, DeviceBody body) {
        Map<String,Object> before=get(id);scopes.require(((Number)before.get("dataScopeId")).longValue());scopes.require(body.dataScopeId());
        String protocol=dictionaries.requireEnabled("DEVICE_PROTOCOL",body.protocol(),"设备协议");
        jdbc.sql("""
                UPDATE device SET device_code=:code,device_name=:name,device_type=:type,model=:model,
                protocol=:protocol,connection_config='{}'::jsonb,connection_config_ciphertext=:configCipher,data_scope_id=:scope,updated_time=now() WHERE id=:id
                """).param("code",body.deviceCode()).param("name",body.deviceName()).param("type",body.deviceType())
                .param("model",body.model()).param("protocol",protocol).param("configCipher",cipher.encrypt(json.write(body.connectionConfig())))
                .param("scope",body.dataScopeId()).param("id",id).update(); audit.record("UPDATE","DEVICE","更新设备",Map.of("deviceId",id,"before",deviceAuditSnapshot(before),"after",deviceAuditSnapshot(get(id))));
    }

    @Transactional
    public long ingest(long id, java.util.UUID sourceEventId, MeasurementBody body) {
        Map<String,Object> device=get(id);scopes.require(((Number)device.get("dataScopeId")).longValue());UserPrincipal actor=CurrentUser.require();return ingestInternal(id,sourceEventId,body,actor.id(),"user:"+actor.username(),"USER_JWT");
    }

    public VerifiedDevice verifyDeviceRequest(String deviceCode,java.util.UUID sourceEventId,long timestamp,String signature,String rawBody){
        DeviceCredential credential=jdbc.sql("SELECT id,device_code,ingest_secret_ciphertext FROM device WHERE device_code=:code")
                .param("code",deviceCode).query(DeviceCredential.class).optional().orElseThrow(()->new BusinessException(401,"设备身份验证失败"));
        if(credential.ingestSecretCiphertext()==null)throw new BusinessException(401,"设备采集密钥尚未配置");
        long now=Instant.now().getEpochSecond();if(Math.abs(now-timestamp)>300)throw new BusinessException(401,"设备签名时间已过期");
        String expected=hmac(cipher.decrypt(credential.ingestSecretCiphertext()),timestamp+"."+sourceEventId+"."+rawBody);
        if(signature==null||!constantTimeEquals(expected,signature.trim().toLowerCase(java.util.Locale.ROOT)))throw new BusinessException(401,"设备签名不正确");
        return new VerifiedDevice(credential.id(),credential.deviceCode());
    }

    @Transactional
    public long ingestVerifiedDevice(VerifiedDevice verified,java.util.UUID sourceEventId,MeasurementBody body){return ingestInternal(verified.id(),sourceEventId,body,null,"device:"+verified.deviceCode(),"DEVICE_HMAC");}

    @Transactional
    public long ingestAdapter(long id,java.util.UUID sourceEventId,MeasurementBody body,String adapter){
        return ingestInternal(id,sourceEventId,body,null,"adapter:"+adapter,"ADAPTER");
    }

    private long ingestInternal(long id,java.util.UUID sourceEventId,MeasurementBody body,Long actorId,String principal,String mode) {
        if(body==null||body.metricName()==null||body.metricName().isBlank()||body.metricName().length()>100)throw BusinessException.badRequest("测点名称不能为空且最长100字符");
        if(body.metricValue()==null&&(body.textValue()==null||body.textValue().isBlank()))throw BusinessException.badRequest("数值和文本值至少填写一项");
        if(body.quality()!=null&&!java.util.Set.of("GOOD","UNCERTAIN","BAD").contains(body.quality()))throw BusinessException.badRequest("质量标识不正确");
        if(body.rawData()==null)throw BusinessException.badRequest("原始报文不能为空");
        Instant received=Instant.now(),measured=body.measuredTime()==null?received:body.measuredTime();
        if(measured.isAfter(received.plusSeconds(300)))throw BusinessException.badRequest("设备时间不能超过服务器时间5分钟");
        boolean transportAuthenticated=!"USER_JWT".equals(mode);
        String previousStatus=jdbc.sql("SELECT status FROM device WHERE id=:id"+(transportAuthenticated?" FOR UPDATE":"")).param("id",id).query(String.class).optional().orElseThrow(()->BusinessException.notFound("设备不存在"));
        String fingerprint=measurementFingerprint(id,body);
        java.util.Optional<Long> inserted=jdbc.sql("""
                INSERT INTO device_measurement(device_id,metric_name,metric_value,text_value,unit,quality,measured_time,raw_data,
                                               source_event_id,created_by,request_fingerprint,source_principal,source_mode,raw_digest,received_time)
                VALUES (:device,:metric,:number,:text,:unit,:quality,:measured,CAST(:raw AS jsonb),:sourceEvent,:createdBy,
                        :fingerprint,:principal,:mode,:rawDigest,:received)
                ON CONFLICT (device_id,source_event_id) WHERE source_event_id IS NOT NULL DO NOTHING RETURNING id
                """).param("device",id).param("metric",body.metricName()).param("number",body.metricValue())
                .param("text",body.textValue()).param("unit",body.unit()).param("quality",body.quality()==null?"GOOD":body.quality())
                .param("measured",Timestamp.from(measured)).param("raw",json.write(body.rawData())).param("sourceEvent",sourceEventId)
                .param("createdBy",actorId,java.sql.Types.BIGINT).param("fingerprint",fingerprint).param("principal",principal)
                .param("mode",mode).param("rawDigest",sha256(json.canonical(body.rawData()))).param("received",Timestamp.from(received)).query(Long.class).optional();
        if(inserted.isEmpty()){
            MeasurementIdentity existing=jdbc.sql("SELECT id,request_fingerprint FROM device_measurement WHERE device_id=:device AND source_event_id=:sourceEvent")
                    .param("device",id).param("sourceEvent",sourceEventId).query(MeasurementIdentity.class).single();
            if(!fingerprint.equals(existing.requestFingerprint()))throw new BusinessException(409,"幂等键已用于其他设备测点请求");
            if(transportAuthenticated)markTransportOnline(id,received,mode,actorId,principal,previousStatus);
            audit.recordAs(actorId,principal,"INGEST_REPLAY","DEVICE","重复设备事件按幂等键返回既有测点",Map.of("deviceId",id,
                    "measurementId",existing.id(),"metricName",body.metricName(),"sourceEventId",sourceEventId.toString()));
            return existing.id();
        }
        if(transportAuthenticated)markTransportOnline(id,received,mode,actorId,principal,previousStatus);
        audit.recordAs(actorId,principal,"INGEST","DEVICE","接收设备测点数据",Map.of("deviceId",id,"measurementId",inserted.get(),
                "metricName",body.metricName(),"measuredTime",measured.toString(),"receivedTime",received.toString(),"quality",body.quality()==null?"GOOD":body.quality(),"sourceMode",mode));
        return inserted.get();
    }

    private void markTransportOnline(long id,Instant received,String mode,Long actorId,String principal,String previousStatus){jdbc.sql("""
                UPDATE device SET status='ONLINE',last_seen_time=:received,updated_time=now(),
                    last_status_change_time=CASE WHEN status<>'ONLINE' THEN now() ELSE last_status_change_time END,
                    last_status_reason=CASE WHEN status<>'ONLINE' THEN :reason ELSE last_status_reason END
                WHERE id=:id
                """).param("received",Timestamp.from(received)).param("reason","收到"+mode+"测点，连接恢复").param("id",id).update();
        if(!"ONLINE".equals(previousStatus))audit.recordAs(actorId,principal,"DEVICE_ONLINE","DEVICE","设备连接恢复",Map.of("deviceId",id,"fromStatus",previousStatus,"toStatus","ONLINE","sourceMode",mode));
    }

    public PageResponse<Map<String,Object>> measurements(long id,String metric,Instant from,Instant to,int pageNum,int pageSize) {
        get(id);if(from!=null&&to!=null&&!from.isBefore(to))throw BusinessException.badRequest("开始时间必须早于结束时间");int page=Math.max(1,pageNum),size=Math.max(1,Math.min(pageSize,500));
        String where=" FROM device_measurement WHERE device_id=:id AND (CAST(:metric AS text) IS NULL OR metric_name=CAST(:metric AS text)) AND (CAST(:from AS timestamptz) IS NULL OR measured_time>=CAST(:from AS timestamptz)) AND (CAST(:to AS timestamptz) IS NULL OR measured_time<CAST(:to AS timestamptz))";
        long total=params(jdbc.sql("SELECT count(*)"+where),id,metric,from,to).query(Long.class).single();
        var rows=params(jdbc.sql("SELECT *"+where+" ORDER BY measured_time DESC LIMIT :limit OFFSET :offset"),id,metric,from,to)
                .param("limit",size).param("offset",(page-1)*size).query((rs,n)->{
                    Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("metricName",rs.getString("metric_name"));
                    v.put("metricValue",rs.getObject("metric_value"));v.put("textValue",rs.getString("text_value"));v.put("unit",rs.getString("unit"));
                    v.put("quality",rs.getString("quality"));v.put("measuredTime",rs.getObject("measured_time"));v.put("receivedTime",rs.getObject("received_time"));
                    v.put("sourcePrincipal",rs.getString("source_principal"));v.put("sourceMode",rs.getString("source_mode"));v.put("rawDigest",rs.getString("raw_digest"));v.put("rawData",json.map(rs.getString("raw_data")));return v;
                }).list();return PageResponse.of(total,page,size,rows);
    }

    @Transactional
    public DeviceRegistration rotateIngestSecret(long id){Map<String,Object> device=get(id);scopes.require(((Number)device.get("dataScopeId")).longValue());String secret=newSecret();jdbc.sql("UPDATE device SET ingest_secret_ciphertext=:secret,updated_time=now() WHERE id=:id").param("secret",cipher.encrypt(secret)).param("id",id).update();audit.record("ROTATE_SECRET","DEVICE","轮换设备采集密钥",Map.of("deviceId",id,"deviceCode",device.get("deviceCode")));return new DeviceRegistration(id,secret);}

    @Transactional
    public void configurePolling(long id,boolean enabled,int intervalSeconds,int timeoutSeconds){Map<String,Object> device=get(id);scopes.require(((Number)device.get("dataScopeId")).longValue());String protocol=String.valueOf(device.get("protocol")).toUpperCase(java.util.Locale.ROOT);if(enabled)protocol=dictionaries.requireEnabled("DEVICE_PROTOCOL",protocol,"设备协议");if(enabled&&!"SIMULATOR".equals(protocol))throw new BusinessException(501,"当前版本只内置SIMULATOR适配器；"+protocol+"需完成厂商协议适配和联调后启用");if(intervalSeconds<1||intervalSeconds>86400)throw BusinessException.badRequest("轮询间隔须为1至86400秒");if(timeoutSeconds<10||timeoutSeconds>86400)throw BusinessException.badRequest("心跳超时须为10至86400秒");jdbc.sql("UPDATE device SET adapter_enabled=:enabled,poll_interval_seconds=:interval,heartbeat_timeout_seconds=:timeout,next_poll_time=CASE WHEN :enabled THEN now() ELSE NULL END,updated_time=now() WHERE id=:id").param("enabled",enabled).param("interval",intervalSeconds).param("timeout",timeoutSeconds).param("id",id).update();audit.record(enabled?"ENABLE_ADAPTER":"DISABLE_ADAPTER","DEVICE",enabled?"启用设备自动采集":"停用设备自动采集",Map.of("deviceId",id,"protocol",protocol,"pollIntervalSeconds",intervalSeconds,"heartbeatTimeoutSeconds",timeoutSeconds,"before",deviceAuditSnapshot(device),"after",deviceAuditSnapshot(get(id))));}

    @Transactional
    public List<PollingTarget> claimPollingTargets(int limit){return jdbc.sql("""
            WITH due AS (
                SELECT id FROM device WHERE adapter_enabled=TRUE AND next_poll_time<=now()
                ORDER BY next_poll_time,id FOR UPDATE SKIP LOCKED LIMIT :limit
            )
            UPDATE device d SET next_poll_time=now()+d.poll_interval_seconds*interval '1 second'
            FROM due WHERE d.id=due.id
            RETURNING d.id,d.device_code,d.protocol,d.connection_config_ciphertext,d.poll_interval_seconds,d.heartbeat_timeout_seconds
            """).param("limit",Math.max(1,Math.min(limit,100))).query(this::pollingTarget).list();}

    @Transactional
    public PollingTarget claimPollingTarget(long id){return jdbc.sql("""
            UPDATE device SET next_poll_time=now()+poll_interval_seconds*interval '1 second'
            WHERE id=:id AND adapter_enabled=TRUE
            RETURNING id,device_code,protocol,connection_config_ciphertext,poll_interval_seconds,heartbeat_timeout_seconds
            """).param("id",id).query(this::pollingTarget).optional().orElseThrow(()->BusinessException.badRequest("设备自动采集未启用"));}

    public void adapterFailed(PollingTarget target,String reason){String safe=reason==null?"适配器采集失败":reason.substring(0,Math.min(reason.length(),450));int changed=jdbc.sql("UPDATE device SET status='OFFLINE',last_status_change_time=CASE WHEN status<>'OFFLINE' THEN now() ELSE last_status_change_time END,last_status_reason=:reason,updated_time=now() WHERE id=:id AND status<>'OFFLINE'").param("reason",safe).param("id",target.id()).update();if(changed>0)audit.recordAs(null,"adapter:"+target.protocol(),"DEVICE_OFFLINE","DEVICE","适配器失败导致设备离线",Map.of("deviceId",target.id(),"deviceCode",target.deviceCode(),"reason",safe));}

    public int markTimedOutDevices(Instant now){List<OfflineDevice> changed=jdbc.sql("""
            WITH offline AS (
                UPDATE device SET status='OFFLINE',last_status_change_time=:now,
                    last_status_reason='心跳超过阈值，自动判定离线',updated_time=:now
                WHERE status='ONLINE' AND last_seen_time IS NOT NULL
                  AND last_seen_time < (CAST(:now AS timestamptz) - heartbeat_timeout_seconds*interval '1 second')
                RETURNING id,device_code,heartbeat_timeout_seconds,last_seen_time
            ) SELECT * FROM offline ORDER BY id
            """).param("now",Timestamp.from(now)).query(OfflineDevice.class).list();for(OfflineDevice device:changed)audit.recordAs(null,"system","DEVICE_OFFLINE","DEVICE","设备心跳超时离线",Map.of("deviceId",device.id(),"deviceCode",device.deviceCode(),"heartbeatTimeoutSeconds",device.heartbeatTimeoutSeconds(),"lastSeenTime",device.lastSeenTime().toString()));return changed.size();}

    private JdbcClient.StatementSpec params(JdbcClient.StatementSpec s,long id,String metric,Instant from,Instant to){
        return s.param("id",id).param("metric",blank(metric)).param("from",from==null?null:Timestamp.from(from)).param("to",to==null?null:Timestamp.from(to));
    }
    private Map<String,Object> row(ResultSet rs,int n)throws SQLException{
        Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("deviceCode",rs.getString("device_code"));
        v.put("deviceName",rs.getString("device_name"));v.put("deviceType",rs.getString("device_type"));v.put("model",rs.getString("model"));
        v.put("protocol",rs.getString("protocol"));v.put("status",rs.getString("status"));
        String encrypted=rs.getString("connection_config_ciphertext");
        v.put("connectionConfig",redact(json.map(encrypted==null?rs.getString("connection_config"):cipher.decrypt(encrypted))));
        v.put("dataScopeId",rs.getLong("data_scope_id"));v.put("lastSeenTime",rs.getObject("last_seen_time"));
        v.put("adapterEnabled",rs.getBoolean("adapter_enabled"));v.put("pollIntervalSeconds",rs.getInt("poll_interval_seconds"));v.put("heartbeatTimeoutSeconds",rs.getInt("heartbeat_timeout_seconds"));v.put("lastStatusChangeTime",rs.getObject("last_status_change_time"));v.put("lastStatusReason",rs.getString("last_status_reason"));return v;
    }
    private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private String measurementFingerprint(long deviceId,MeasurementBody body){
        try{Map<String,Object> value=new LinkedHashMap<>();value.put("deviceId",deviceId);value.put("metricName",body.metricName());value.put("metricValue",body.metricValue());value.put("textValue",body.textValue());value.put("unit",body.unit());value.put("quality",body.quality()==null?"GOOD":body.quality());value.put("measuredTime",body.measuredTime()==null?"":body.measuredTime().toString());value.put("rawData",body.rawData());return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.canonical(value).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}
    }
    private PollingTarget pollingTarget(ResultSet rs,int n)throws SQLException{String encrypted=rs.getString("connection_config_ciphertext");Map<String,Object> config=encrypted==null?Map.of():json.map(cipher.decrypt(encrypted));return new PollingTarget(rs.getLong("id"),rs.getString("device_code"),rs.getString("protocol"),config,rs.getInt("poll_interval_seconds"),rs.getInt("heartbeat_timeout_seconds"));}
    private String newSecret(){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private String hmac(String secret,String value){try{javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private boolean constantTimeEquals(String expected,String actual){return java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
    private String sha256(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    @SuppressWarnings("unchecked") private Object redact(Object value){
        if(value instanceof Map<?,?> map){Map<String,Object> safe=new LinkedHashMap<>();map.forEach((k,v)->{String key=String.valueOf(k);safe.put(key,key.toLowerCase().matches(".*(password|secret|token|credential|api.?key).*" )?"******":redact(v));});return safe;}
        if(value instanceof List<?> list)return list.stream().map(this::redact).toList();return value;
    }
    private Map<String,Object> deviceAuditSnapshot(Map<String,Object> source){Map<String,Object> value=new LinkedHashMap<>();
        for(String key:List.of("id","deviceCode","deviceName","deviceType","model","protocol","connectionConfig","dataScopeId",
                "adapterEnabled","pollIntervalSeconds","heartbeatTimeoutSeconds"))value.put(key,source.get(key));
        return value;}
    public record DeviceBody(String deviceCode,String deviceName,String deviceType,String model,String protocol,Map<String,Object> connectionConfig,long dataScopeId){}
    public record MeasurementBody(String metricName,java.math.BigDecimal metricValue,String textValue,String unit,String quality,Instant measuredTime,Map<String,Object> rawData){}
    public record DeviceRegistration(long id,String deviceSecret){}
    public record PollingTarget(long id,String deviceCode,String protocol,Map<String,Object> connectionConfig,int pollIntervalSeconds,int heartbeatTimeoutSeconds){}
    private record DeviceCredential(long id,String deviceCode,String ingestSecretCiphertext){}
    public record VerifiedDevice(long id,String deviceCode){}
    private record OfflineDevice(long id,String deviceCode,int heartbeatTimeoutSeconds,Instant lastSeenTime){}
    private record MeasurementIdentity(long id,String requestFingerprint){}
}
