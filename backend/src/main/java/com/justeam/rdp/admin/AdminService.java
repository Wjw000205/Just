package com.justeam.rdp.admin;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.TokenService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final JdbcClient jdbc; private final AuditService audit;private final TokenService tokens;
    public AdminService(JdbcClient jdbc,AuditService audit,TokenService tokens){this.jdbc=jdbc;this.audit=audit;this.tokens=tokens;}

    public PageResponse<Map<String,Object>> users(String keyword,Integer status,Long departmentId,int pageNum,int pageSize,boolean exposeManagedAccounts){
        int page=Math.max(1,pageNum),size=Math.max(1,Math.min(200,pageSize));
        String registrationVisibility=exposeManagedAccounts?"TRUE":"NOT EXISTS (SELECT 1 FROM sys_registration_application ra WHERE ra.user_id=u.id AND ra.status IN ('PENDING','REJECTED'))";
        String where=" FROM sys_user u LEFT JOIN sys_department d ON d.id=u.department_id WHERE u.deleted=0 AND "+registrationVisibility+" AND (CAST(:keyword AS text) IS NULL OR lower(u.username) LIKE lower(concat('%',CAST(:keyword AS text),'%')) OR lower(u.real_name) LIKE lower(concat('%',CAST(:keyword AS text),'%'))) AND (CAST(:status AS integer) IS NULL OR u.status=CAST(:status AS integer)) AND (CAST(:department AS bigint) IS NULL OR u.department_id=CAST(:department AS bigint))";
        long total=jdbc.sql("SELECT count(*)"+where).param("keyword",blank(keyword)).param("status",status).param("department",departmentId).query(Long.class).single();
        List<Map<String,Object>> rows=jdbc.sql("""
                SELECT u.id,u.username,u.real_name,u.email,u.phone,u.status,u.must_change_password,u.created_time,u.department_id,
                       d.dept_name AS department,u.authorization_version,
                       ARRAY(SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=u.id ORDER BY r.sort_order) roles,
                       ARRAY(SELECT data_scope_id FROM sys_user_data_scope WHERE user_id=u.id ORDER BY data_scope_id) scopes
                """+where+" ORDER BY u.created_time DESC,u.id DESC LIMIT :limit OFFSET :offset").param("keyword",blank(keyword)).param("status",status).param("department",departmentId)
                .param("limit",size).param("offset",(page-1)*size).query((rs,n)->userRow(rs,n,exposeManagedAccounts)).list();
        return PageResponse.of(total,page,size,rows);
    }

    public Map<String,Object> options(){return Map.of(
            "roles",grantableRoleOptions(),
            "scopes",grantableScopeOptions(),
            "departments",grantableDepartmentOptions());}

    @Transactional public void setStatus(long id,int status,String reason){
        adminMutationLock();
        referenceLockShared(4204);
        referenceLockExclusive(4207);
        requireManageableTarget(id);
        if(status!=0&&status!=1)throw BusinessException.badRequest("状态只支持启用或停用");
        if(id==CurrentUser.require().id()&&status==0)throw BusinessException.badRequest("不能停用当前账号");
        if(status==1){String registrationStatus=jdbc.sql("SELECT status FROM sys_registration_application WHERE user_id=:user").param("user",id).query(String.class).optional().orElse(null);if(registrationStatus!=null&&!"APPROVED".equals(registrationStatus))throw new BusinessException(409,"自助注册账号必须通过注册审核流程后才能启用");}
        if(status==0&&isSoleAdmin(id))throw BusinessException.badRequest("不能停用唯一的启用管理员");
        if(status==0&&departmentLeadershipCount(id)>0)throw new BusinessException(409,"该用户仍是部门负责人，请先完成负责人移交");
        Integer before=jdbc.sql("SELECT status FROM sys_user WHERE id=:id AND deleted=0").param("id",id).query(Integer.class).optional().orElseThrow(()->BusinessException.notFound("用户不存在"));
        int updated=jdbc.sql("UPDATE sys_user SET status=:status,updated_by=:actor,updated_time=now() WHERE id=:id AND deleted=0")
                .param("status",status).param("actor",CurrentUser.require().id()).param("id",id).update();
        if(updated==0)throw BusinessException.notFound("用户不存在");tokens.revoke(id);audit.record("STATUS","USER","变更用户状态并撤销目标账号会话",Map.of("userId",id,"before",before,"after",status,"reason",reason));
    }

    @Transactional public void assign(long id,List<Long> roleIds,List<Long> scopeIds,Long departmentId,String reason,int expectedVersion){
        adminMutationLock();
        referenceLockShared(4203);referenceLockShared(4204);referenceLockShared(4205);
        requireManageableTarget(id);
        if(jdbc.sql("SELECT count(*) FROM sys_user WHERE id=:id AND deleted=0").param("id",id).query(Long.class).single()==0)throw BusinessException.notFound("用户不存在");
        if(roleIds==null||roleIds.isEmpty())throw BusinessException.badRequest("至少分配一个角色");
        if(roleIds.stream().distinct().count()!=roleIds.size()||(scopeIds!=null&&scopeIds.stream().distinct().count()!=scopeIds.size()))throw BusinessException.badRequest("角色或数据域不能重复");
        long validRoles=jdbc.sql("SELECT count(*) FROM sys_role WHERE id IN (:ids) AND status=1 AND deleted=0").param("ids",roleIds).query(Long.class).single();
        if(validRoles!=roleIds.stream().distinct().count())throw BusinessException.badRequest("包含无效角色");
        requireGrantableRoles(roleIds);
        if(scopeIds!=null&&!scopeIds.isEmpty()){long valid=jdbc.sql("SELECT count(*) FROM sys_data_scope WHERE id IN (:ids) AND active=TRUE").param("ids",scopeIds).query(Long.class).single();if(valid!=scopeIds.stream().distinct().count())throw BusinessException.badRequest("包含无效数据域");}
        requireGrantableScopes(scopeIds);
        if(departmentId!=null){long valid=jdbc.sql("SELECT count(*) FROM sys_department WHERE id=:id AND status=1 AND deleted=0").param("id",departmentId).query(Long.class).single();if(valid==0)throw BusinessException.badRequest("部门不存在或已停用");}
        requireGrantableDepartment(departmentId);
        Map<String,Object> before=userAssignments(id);
        boolean targetActiveAdmin=jdbc.sql("SELECT count(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id WHERE u.id=:id AND u.status=1 AND r.role_code='ADMIN'").param("id",id).query(Long.class).single()>0;
        boolean retainsAdmin=jdbc.sql("SELECT count(*) FROM sys_role WHERE id IN (:ids) AND role_code='ADMIN'").param("ids",roleIds).query(Long.class).single()>0;
        if(targetActiveAdmin&&!retainsAdmin&&activeAdminCount()<=1)throw BusinessException.badRequest("不能移除唯一启用管理员的ADMIN角色");
        jdbc.sql("DELETE FROM sys_user_role WHERE user_id=:id").param("id",id).update();
        for(Long role:roleIds)jdbc.sql("INSERT INTO sys_user_role(user_id,role_id) VALUES (:id,:role)").param("id",id).param("role",role).update();
        jdbc.sql("DELETE FROM sys_user_data_scope WHERE user_id=:id").param("id",id).update();
        if(scopeIds!=null)for(Long scope:scopeIds)jdbc.sql("INSERT INTO sys_user_data_scope(user_id,data_scope_id) VALUES (:id,:scope)").param("id",id).param("scope",scope).update();
        int updated=jdbc.sql("UPDATE sys_user SET department_id=:department,authorization_version=authorization_version+1,updated_by=:actor,updated_time=now() WHERE id=:id AND authorization_version=:version").param("department",departmentId).param("actor",CurrentUser.require().id()).param("id",id).param("version",expectedVersion).update();
        if(updated==0)throw new BusinessException(409,"用户授权版本已变化，请刷新后重试");
        tokens.revoke(id);
        audit.record("ASSIGN","USER","分配角色与数据域",Map.of("userId",id,"before",before,"after",userAssignments(id),"reason",reason));
    }

    private boolean isSoleAdmin(long id){return jdbc.sql("SELECT count(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id WHERE u.status=1 AND u.deleted=0 AND r.role_code='ADMIN'").query(Long.class).single()<=1&&jdbc.sql("SELECT count(*) FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=:id AND r.role_code='ADMIN'").param("id",id).query(Long.class).single()>0;}
    private long activeAdminCount(){return jdbc.sql("SELECT count(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id WHERE u.status=1 AND u.deleted=0 AND r.role_code='ADMIN'").query(Long.class).single();}
    private long departmentLeadershipCount(long userId){return jdbc.sql("SELECT count(*) FROM sys_department WHERE leader_id=:id AND deleted=0").param("id",userId).query(Long.class).single();}
    private void requireGrantableRoles(List<Long> roleIds){var user=CurrentUser.require();if(user.admin())return;long held=jdbc.sql("SELECT count(DISTINCT ur.role_id) FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0 WHERE ur.user_id=:user AND ur.role_id IN (:ids)").param("user",user.id()).param("ids",roleIds).query(Long.class).single();if(held!=roleIds.stream().distinct().count())throw new BusinessException(403,"非系统管理员只能授予自己当前持有的角色");List<String> permissions=jdbc.sql("SELECT DISTINCT p.permission_code FROM sys_role_permission rp JOIN sys_permission p ON p.id=rp.permission_id AND p.status=1 AND p.deleted=0 WHERE rp.role_id IN (:ids)").param("ids",roleIds).query(String.class).list();if(!user.permissions().containsAll(permissions))throw new BusinessException(403,"不能授予当前账号自身不具备的角色权限");}
    private void requireGrantableScopes(List<Long> scopeIds){var user=CurrentUser.require();if(user.admin()||scopeIds==null||scopeIds.isEmpty())return;if(!user.assignedScopes().containsAll(scopeIds))throw new BusinessException(403,"不能授予当前账号未直接拥有的数据域");long global=jdbc.sql("SELECT count(*) FROM sys_data_scope WHERE id IN (:ids) AND scope_type='GLOBAL'").param("ids",scopeIds).query(Long.class).single();if(global>0)throw new BusinessException(403,"只有系统管理员可以授予全局数据域");}
    private void requireGrantableDepartment(Long departmentId){var user=CurrentUser.require();if(user.admin()||departmentId==null)return;Long own=jdbc.sql("SELECT department_id FROM sys_user WHERE id=:id AND deleted=0").param("id",user.id()).query(Long.class).optional().orElse(null);if(!java.util.Objects.equals(own,departmentId))throw new BusinessException(403,"不能分配当前账号管理范围外的部门");}
    private List<Map<String,Object>> grantableRoleOptions(){var user=CurrentUser.require();JdbcClient.StatementSpec query;if(user.admin()){query=jdbc.sql("SELECT id,role_code,role_name,description FROM sys_role WHERE deleted=0 AND status=1 ORDER BY sort_order");}else{query=jdbc.sql("SELECT r.id,r.role_code,r.role_name,r.description FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id AND r.deleted=0 AND r.status=1 WHERE ur.user_id=:user ORDER BY r.sort_order").param("user",user.id());}return query.query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"code",rs.getString("role_code"),"name",rs.getString("role_name"),"description",rs.getString("description")==null?"":rs.getString("description"))).list();}
    private List<Map<String,Object>> grantableScopeOptions(){var user=CurrentUser.require();JdbcClient.StatementSpec query;if(user.admin()){query=jdbc.sql("SELECT id,scope_code,scope_name,scope_type FROM sys_data_scope WHERE active=TRUE ORDER BY id");}else if(user.assignedScopes().isEmpty()){return List.of();}else{query=jdbc.sql("SELECT id,scope_code,scope_name,scope_type FROM sys_data_scope WHERE active=TRUE AND scope_type<>'GLOBAL' AND id IN (:ids) ORDER BY id").param("ids",user.assignedScopes());}return query.query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"code",rs.getString("scope_code"),"name",rs.getString("scope_name"),"type",rs.getString("scope_type"))).list();}
    private List<Map<String,Object>> grantableDepartmentOptions(){var user=CurrentUser.require();JdbcClient.StatementSpec query;if(user.admin()){query=jdbc.sql("SELECT id,dept_code,dept_name FROM sys_department WHERE deleted=0 AND status=1 ORDER BY id");}else{Long own=jdbc.sql("SELECT department_id FROM sys_user WHERE id=:id AND deleted=0").param("id",user.id()).query(Long.class).optional().orElse(null);if(own==null)return List.of();query=jdbc.sql("SELECT id,dept_code,dept_name FROM sys_department WHERE id=:id AND deleted=0 AND status=1").param("id",own);}return query.query((rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"code",rs.getString("dept_code"),"name",rs.getString("dept_name"))).list();}
    private void requireManageableTarget(long userId){var actor=CurrentUser.require();if(actor.admin())return;long admin=jdbc.sql("SELECT count(*) FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=:id AND r.role_code='ADMIN' AND r.status=1 AND r.deleted=0").param("id",userId).query(Long.class).single();List<String> permissions=jdbc.sql("SELECT DISTINCT p.permission_code FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0 JOIN sys_role_permission rp ON rp.role_id=r.id JOIN sys_permission p ON p.id=rp.permission_id AND p.status=1 AND p.deleted=0 WHERE ur.user_id=:id").param("id",userId).query(String.class).list();if(admin>0||!actor.permissions().containsAll(permissions))throw new BusinessException(403,"不能管理权限高于当前账号的用户");}
    private void adminMutationLock(){jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(34882201)) x").query(Long.class).single();}
    private void referenceLockShared(long key){jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock_shared(:key)) locked").param("key",key).query(Long.class).single();}
    private void referenceLockExclusive(long key){jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(:key)) locked").param("key",key).query(Long.class).single();}
    private Map<String,Object> userAssignments(long id){List<String> roles=jdbc.sql("SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=:id ORDER BY r.role_code").param("id",id).query(String.class).list();List<Long> scopes=jdbc.sql("SELECT data_scope_id FROM sys_user_data_scope WHERE user_id=:id ORDER BY data_scope_id").param("id",id).query(Long.class).list();Long dept=jdbc.sql("SELECT department_id FROM sys_user WHERE id=:id").param("id",id).query(Long.class).optional().orElse(null);Map<String,Object> v=new LinkedHashMap<>();v.put("roles",roles);v.put("scopes",scopes);v.put("departmentId",dept);return v;}
    private Map<String,Object> userRow(ResultSet rs,int n,boolean exposeContacts)throws SQLException{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("username",rs.getString("username"));v.put("realName",rs.getString("real_name"));v.put("email",exposeContacts?rs.getString("email"):mask(rs.getString("email")));v.put("phone",exposeContacts?rs.getString("phone"):mask(rs.getString("phone")));v.put("status",rs.getInt("status"));v.put("mustChangePassword",rs.getBoolean("must_change_password"));v.put("departmentId",rs.getObject("department_id"));v.put("department",rs.getString("department"));v.put("roles",strings(rs.getArray("roles")));v.put("dataScopes",longs(rs.getArray("scopes")));v.put("version",rs.getInt("authorization_version"));v.put("createdTime",rs.getObject("created_time"));return v;}
    private List<String> strings(Array a)throws SQLException{return Arrays.stream((Object[])a.getArray()).map(Object::toString).toList();}
    private List<Long> longs(Array a)throws SQLException{return Arrays.stream((Object[])a.getArray()).map(v->((Number)v).longValue()).toList();}
    private String mask(String value){if(value==null||value.isBlank())return null;int at=value.indexOf('@');if(at>0)return value.substring(0,Math.min(2,at))+"***"+value.substring(at);return value.length()>=7?value.substring(0,3)+"****"+value.substring(value.length()-4):"***";}
    private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
}
