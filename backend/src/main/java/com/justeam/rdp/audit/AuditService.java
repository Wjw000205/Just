package com.justeam.rdp.audit;

import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.RdpProperties;
import com.justeam.rdp.security.UserPrincipal;
import com.justeam.rdp.common.JsonSupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import com.justeam.rdp.common.PageResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.stream.Stream;

@Service
public class AuditService {
    private final JdbcClient jdbc;
    private final RdpProperties properties;
    private final JsonSupport json;
    private final org.springframework.transaction.support.TransactionTemplate transactions;
    private final org.springframework.transaction.support.TransactionTemplate independentTransactions;

    public AuditService(JdbcClient jdbc, RdpProperties properties, JsonSupport json,
                        org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.json = json;
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.independentTransactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.independentTransactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public synchronized void record(String operation, String module, String description) {
        record(operation, module, description, Map.of());
    }

    @Transactional
    public synchronized void record(String operation, String module, String description, Map<String, Object> details) {
        UserPrincipal user;
        try {
            user = CurrentUser.require();
        } catch (Exception ex) {
            user = new UserPrincipal(0, "anonymous", "匿名用户", "", true, false, java.util.List.of(), java.util.List.of(), java.util.Set.of(), java.util.Set.of());
        }
        recordAs(user.id() == 0 ? null : user.id(), user.username(), operation, module, description, details);
    }

    @Transactional
    public synchronized void recordAs(Long actorId, String actorUsername, String operation, String module,
                                      String description, Map<String, Object> details) {
        jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(82467701)) audit_lock")
                .query(Long.class).single();
        String previous = jdbc.sql("SELECT record_digest FROM sys_audit_log ORDER BY id DESC LIMIT 1")
                .query(String.class).optional().orElse("GENESIS");
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Map<String, Object> canonicalDetails = details == null ? Map.of() : details;
        String detailJson = json.canonical(canonicalDetails);
        Map<String, Object> signed = new LinkedHashMap<>();
        signed.put("previousDigest", previous); signed.put("actorId", actorId);
        signed.put("actorUsername", actorUsername == null ? "system" : actorUsername);
        signed.put("operation", operation); signed.put("module", module); signed.put("description", description);
        signed.put("details", canonicalDetails); signed.put("createdTime", now.toString());
        signed.put("auditKeyId", properties.security().auditKeyId());
        String signedPayload = json.canonical(signed);
        String digest = hmac(properties.security().auditSecret(), signedPayload);
        jdbc.sql("""
                INSERT INTO sys_audit_log(user_id, username, operation_type, module, description,
                                          previous_digest, record_digest, audit_key_id, details, signed_payload, created_time)
                VALUES (:userId, :username, :operation, :module, :description, :previous, :digest,
                        :auditKeyId,CAST(:details AS jsonb),:signedPayload,:created)
                """)
                .param("userId", actorId)
                .param("username", actorUsername == null ? "system" : actorUsername)
                .param("operation", operation).param("module", module)
                .param("description", description).param("previous", previous).param("digest", digest)
                .param("auditKeyId", properties.security().auditKeyId())
                .param("details", detailJson)
                .param("signedPayload", signedPayload)
                .param("created", java.sql.Timestamp.from(now)).update();
    }

    /**
     * Records a security event in an independent transaction so a rejected business operation cannot roll it back.
     */
    public void recordIndependent(Long actorId, String actorUsername, String operation, String module,
                                  String description, Map<String, Object> details) {
        independentTransactions.executeWithoutResult(status ->
                recordAs(actorId, actorUsername, operation, module, description, details));
    }

    private String hmac(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public PageResponse<Map<String, Object>> list(Long userId, String module, String username, String operation,
                                                  String keyword, Instant from, Instant to, int pageNum,
                                                  int pageSize, String sortOrder) {
        validateFilters(module, operation, from, to);
        int page = Math.max(pageNum, 1), size = Math.max(1, Math.min(pageSize, 200));
        String direction = normalizeSort(sortOrder);
        String where = where();
        long total = filter(jdbc.sql("SELECT count(*) FROM sys_audit_log" + where), userId, module, username,
                operation, keyword, from, to, null).query(Long.class).single();
        JdbcClient.StatementSpec listSpec = filter(jdbc.sql("""
                SELECT id,user_id,username,operation_type,module,description,details::text AS details,
                       audit_key_id,previous_digest,record_digest,created_time
                FROM sys_audit_log
                """ + where + " ORDER BY created_time " + direction + ", id " + direction
                + " LIMIT :limit OFFSET :offset"), userId, module, username, operation, keyword, from, to, null)
                .param("limit", size).param("offset", (page - 1) * size);
        List<Map<String, Object>> rows = listSpec.query((rs, n) -> auditRow(rs)).list();
        return PageResponse.of(total, page, size, rows);
    }

    public ExportPlan prepareExport(String format, Long userId, String module, String username, String operation,
                                    String keyword, Instant from, Instant to) {
        validateFilters(module, operation, from, to);
        UserPrincipal actor = CurrentUser.require();
        long maxId = jdbc.sql("SELECT coalesce(max(id),0) FROM sys_audit_log").query(Long.class).single();
        return new ExportPlan(format, userId, blank(module), blank(username), blank(operation), blank(keyword),
                from, to, maxId, actor.id(), actor.username());
    }

    public void export(OutputStream output, ExportPlan plan) throws IOException {
        String sql = """
                SELECT id,user_id,username,operation_type,module,description,details::text AS details,
                       audit_key_id,previous_digest,record_digest,created_time
                FROM sys_audit_log
                """ + where() + " ORDER BY id";
        long rowCount = 0;
        try {
            rowCount = "xlsx".equals(plan.format()) ? writeXlsx(output, sql, plan) : writeCsv(output, sql, plan);
            long exportedRows=rowCount;
            transactions.executeWithoutResult(status -> recordAs(plan.actorId(), plan.actorUsername(), "EXPORT", "AUDIT",
                    "导出审计日志", exportDetails(plan, exportedRows, "SUCCESS")));
        } catch (IOException | RuntimeException ex) {
            try {
                long exportedRows=rowCount;
                transactions.executeWithoutResult(status -> recordAs(plan.actorId(), plan.actorUsername(), "EXPORT_FAILED", "AUDIT",
                        "审计日志导出失败", exportDetails(plan, exportedRows, "FAILED")));
            } catch (Exception ignored) { }
            throw ex;
        }
    }

    private long writeCsv(OutputStream output, String sql, ExportPlan plan) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.write("\uFEFF编号,发生时间,用户,模块,操作,说明,详情,密钥版本,前序摘要,记录摘要\n");
        long count = 0;
        try (Stream<Map<String,Object>> rows = filter(jdbc.sql(sql), plan.userId(), plan.module(), plan.username(),
                plan.operation(), plan.keyword(), plan.from(), plan.to(), plan.maxId())
                .query((rs,n)->auditRow(rs)).stream()) {
            var iterator = rows.iterator();
            while (iterator.hasNext()) {
                Map<String,Object> row = iterator.next();
                writer.write(String.join(",", csv(row.get("id")), csv(row.get("createdTime")), csv(row.get("username")),
                        csv(row.get("module")), csv(row.get("operation")), csv(row.get("description")),
                        csv(json.write(row.get("details"))), csv(row.get("auditKeyId")),
                        csv(row.get("previousDigest")), csv(row.get("recordDigest"))));
                writer.write('\n');
                count++;
            }
        }
        writer.flush();
        return count;
    }

    private long writeXlsx(OutputStream output, String sql, ExportPlan plan) throws IOException {
        org.apache.poi.xssf.streaming.SXSSFWorkbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        int index = 1;
        try {
            var sheet = workbook.createSheet("audit");
            String[] headers = {"编号","发生时间","用户","模块","操作","说明","详情","密钥版本","前序摘要","记录摘要"};
            var header = sheet.createRow(0); for (int i=0;i<headers.length;i++) header.createCell(i).setCellValue(headers[i]);
            try (Stream<Map<String,Object>> rows = filter(jdbc.sql(sql), plan.userId(), plan.module(), plan.username(),
                    plan.operation(), plan.keyword(), plan.from(), plan.to(), plan.maxId())
                    .query((rs,n)->auditRow(rs)).stream()) {
                var iterator = rows.iterator();
                while (iterator.hasNext()) {
                    Map<String,Object> value = iterator.next(); var row = sheet.createRow(index++);
                    Object[] cells = {value.get("id"),value.get("createdTime"),value.get("username"),value.get("module"),
                            value.get("operation"),value.get("description"),json.write(value.get("details")),
                            value.get("auditKeyId"),value.get("previousDigest"),value.get("recordDigest")};
                    for (int i=0;i<cells.length;i++) row.createCell(i).setCellValue(String.valueOf(cells[i]));
                }
            }
            workbook.write(output); output.flush();
            return index - 1L;
        } finally { workbook.dispose(); workbook.close(); }
    }

    private String where() {
        return """
                 WHERE (CAST(:module AS text) IS NULL OR module=CAST(:module AS text))
                 AND (CAST(:userId AS bigint) IS NULL OR user_id=CAST(:userId AS bigint))
                 AND (CAST(:username AS text) IS NULL OR username=CAST(:username AS text))
                 AND (CAST(:operation AS text) IS NULL OR operation_type=CAST(:operation AS text))
                 AND (CAST(:keyword AS text) IS NULL OR lower(description) LIKE lower(concat('%',CAST(:keyword AS text),'%'))
                      OR lower(details::text) LIKE lower(concat('%',CAST(:keyword AS text),'%')))
                 AND (CAST(:from AS timestamptz) IS NULL OR created_time>=CAST(:from AS timestamptz))
                 AND (CAST(:to AS timestamptz) IS NULL OR created_time<CAST(:to AS timestamptz))
                 AND (CAST(:maxId AS bigint) IS NULL OR id<=CAST(:maxId AS bigint))
                """;
    }

    private JdbcClient.StatementSpec filter(JdbcClient.StatementSpec spec, Long userId, String module, String username,
                                             String operation, String keyword, Instant from, Instant to, Long maxId) {
        return spec.param("userId", userId, java.sql.Types.BIGINT)
                .param("module", blank(module), java.sql.Types.VARCHAR)
                .param("username", blank(username), java.sql.Types.VARCHAR)
                .param("operation", blank(operation), java.sql.Types.VARCHAR)
                .param("keyword", blank(keyword), java.sql.Types.VARCHAR)
                .param("from", timestamp(from), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .param("to", timestamp(to), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .param("maxId", maxId, java.sql.Types.BIGINT);
    }

    private Map<String,Object> auditRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id")); value.put("userId", rs.getObject("user_id"));
        value.put("username", rs.getString("username")); value.put("operation", rs.getString("operation_type"));
        value.put("module", rs.getString("module")); value.put("description", rs.getString("description"));
        value.put("details", json.map(rs.getString("details"))); value.put("auditKeyId", rs.getString("audit_key_id"));
        value.put("previousDigest", rs.getString("previous_digest")); value.put("recordDigest", rs.getString("record_digest"));
        value.put("createdTime", rs.getObject("created_time")); return value;
    }

    private Map<String,Object> exportDetails(ExportPlan plan, long rowCount, String result) {
        Map<String,Object> value=new LinkedHashMap<>();value.put("format",plan.format());value.put("userId",plan.userId());
        value.put("module",plan.module());value.put("username",plan.username());value.put("operation",plan.operation());
        value.put("keyword",plan.keyword());value.put("from",plan.from()==null?null:plan.from().toString());
        value.put("to",plan.to()==null?null:plan.to().toString());value.put("snapshotMaxId",plan.maxId());
        value.put("rowCount",rowCount);value.put("result",result);return value;
    }

    public Map<String,Object> queryDetails(Long userId,String module,String username,String operation,String keyword,
                                           Instant from,Instant to,int pageNum,int pageSize,String sortOrder,long total){
        Map<String,Object> value=new LinkedHashMap<>();value.put("userId",userId);value.put("module",blank(module));
        value.put("username",blank(username));value.put("operation",blank(operation));
        value.put("keywordDigest",keyword==null||keyword.isBlank()?null:sha256(keyword.trim()));
        value.put("from",from==null?null:from.toString());value.put("to",to==null?null:to.toString());
        value.put("pageNum",pageNum);value.put("pageSize",pageSize);value.put("sortOrder",sortOrder);value.put("matchedTotal",total);return value;
    }

    private String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}

    private void validateFilters(String module, String operation, Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw com.justeam.rdp.common.BusinessException.badRequest("开始时间必须早于结束时间");
        }
        validateCode(module, "审计模块"); validateCode(operation, "操作类型");
    }
    private void validateCode(String value,String label){if(value!=null&&!value.isBlank()&&!value.trim().matches("^[A-Z][A-Z0-9_]{0,49}$"))throw com.justeam.rdp.common.BusinessException.badRequest(label+"不正确");}
    private String normalizeSort(String value){String normalized=value==null?"DESC":value.trim().toUpperCase(java.util.Locale.ROOT);if(!java.util.Set.of("ASC","DESC").contains(normalized))throw com.justeam.rdp.common.BusinessException.badRequest("排序方向不正确");return normalized;}
    private java.sql.Timestamp timestamp(Instant value){return value==null?null:java.sql.Timestamp.from(value);}
    private String csv(Object raw){String value=raw==null?"":String.valueOf(raw);int index=0;while(index<value.length()&&(value.charAt(index)<=0x20||Character.isWhitespace(value.charAt(index))))index++;if((!value.isEmpty()&&"\t\r\n".indexOf(value.charAt(0))>=0)||(index<value.length()&&"=+-@".indexOf(value.charAt(index))>=0))value="'"+value;return '"'+value.replace("\"","\"\"")+'"';}

    @Transactional(readOnly = true)
    public Map<String, Object> verifyChain() {
        Map<Long,String> anchors=new java.util.HashMap<>();
        jdbc.sql("SELECT log_id,record_digest FROM sys_audit_anchor ORDER BY log_id")
                .query((rs,n)->{anchors.put(rs.getLong("log_id"),rs.getString("record_digest"));return 1;}).list();
        List<VerifyRow> rows = jdbc.sql("""
                SELECT id,user_id,username,operation_type,module,description,details::text AS details,
                       created_time,previous_digest,record_digest,audit_key_id,signed_payload
                FROM sys_audit_log ORDER BY id
                """).query((rs,n)->new VerifyRow(rs.getLong("id"),(Long)rs.getObject("user_id"),rs.getString("username"),
                        rs.getString("operation_type"),rs.getString("module"),rs.getString("description"),rs.getString("details"),
                        rs.getTimestamp("created_time").toInstant(),rs.getString("previous_digest"),rs.getString("record_digest"),
                        rs.getString("audit_key_id"),rs.getString("signed_payload"))).list();
        if(rows.isEmpty())return verification(false,0,null,"审计链为空，无法证明留存完整性");
        if(anchors.size()!=rows.size())return verification(false,0,null,"审计锚点数量与日志数量不一致");
        String expectedPrevious = "GENESIS";
        long verified = 0;
        for (VerifyRow row : rows) {
            if(!row.recordDigest().equals(anchors.get(row.id())))return verification(false,verified,row.id(),"独立审计锚点缺失或摘要不匹配");
            if (!expectedPrevious.equals(row.previousDigest()))
                return verification(false, verified, row.id(), "前序摘要不匹配");
            String key = auditKey(row.auditKeyId());
            if (row.signedPayload() == null || key == null)
                return verification(false, verified, row.id(), "签名载荷缺失或历史密钥版本未配置");
            Map<String,Object> signed;
            try { signed=json.map(row.signedPayload()); }
            catch (Exception ex) { return verification(false,verified,row.id(),"签名载荷不是有效JSON"); }
            if(!signedPayloadMatches(row,signed)) return verification(false,verified,row.id(),"签名载荷与审计列不一致");
            if (!MessageDigest.isEqual(hmac(key,row.signedPayload()).getBytes(StandardCharsets.UTF_8),
                    row.recordDigest().getBytes(StandardCharsets.UTF_8)))
                return verification(false, verified, row.id(), "记录签名不匹配");
            expectedPrevious = row.recordDigest(); verified++;
        }
        return verification(true, verified, null, "审计链完整");
    }

    private Map<String, Object> verification(boolean valid, long verified, Long failureId, String message) {
        Map<String, Object> value = new LinkedHashMap<>(); value.put("valid", valid);
        value.put("verifiedRecords", verified); value.put("failureId", failureId); value.put("message", message);
        return value;
    }

    private boolean signedPayloadMatches(VerifyRow row,Map<String,Object> signed){
        Object actor=signed.get("actorId");
        boolean actorMatches=row.userId()==null?actor==null:actor instanceof Number n&&n.longValue()==row.userId();
        return actorMatches && row.username().equals(signed.get("actorUsername"))
                && row.operationType().equals(signed.get("operation")) && row.module().equals(signed.get("module"))
                && row.description().equals(signed.get("description"))
                && json.canonical(json.map(row.details())).equals(json.canonical(signed.get("details")))
                && row.createdTime().toString().equals(signed.get("createdTime"))
                && row.auditKeyId().equals(signed.get("auditKeyId"))
                && row.previousDigest().equals(signed.get("previousDigest"));
    }
    private String auditKey(String keyId){
        if(properties.security().auditKeyId().equals(keyId)) return properties.security().auditSecret();
        String previous=properties.security().auditPreviousKeys(); if(previous==null||previous.isBlank())return null;
        for(String entry:previous.split(";")){int split=entry.indexOf('=');if(split>0&&entry.substring(0,split).equals(keyId))return entry.substring(split+1);}
        return null;
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public record ExportPlan(String format, Long userId, String module, String username, String operation,
                             String keyword, Instant from, Instant to, long maxId,
                             long actorId, String actorUsername) {}
    private record VerifyRow(long id,Long userId,String username,String operationType,String module,String description,
                             String details,Instant createdTime,String previousDigest,String recordDigest,String auditKeyId,String signedPayload) {}
}
