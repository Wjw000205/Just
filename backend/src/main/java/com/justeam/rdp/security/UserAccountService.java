package com.justeam.rdp.security;

import com.justeam.rdp.common.BusinessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserAccountService implements UserDetailsService {
    private final JdbcClient jdbc;

    public UserAccountService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserPrincipal loadUserByUsername(String username) {
        UserRow row = jdbc.sql("""
                SELECT id, username, real_name, password, status, must_change_password
                FROM sys_user WHERE username = :username AND deleted = 0
                """).param("username", username).query(UserRow.class).optional()
                .orElseThrow(() -> BusinessException.unauthorized("用户名或密码错误"));
        return enrich(row);
    }

    public UserPrincipal loadById(long id) {
        UserRow row = jdbc.sql("""
                SELECT id, username, real_name, password, status, must_change_password
                FROM sys_user WHERE id = :id AND deleted = 0
                """).param("id", id).query(UserRow.class).optional()
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在或已删除"));
        return enrich(row);
    }

    private UserPrincipal enrich(UserRow row) {
        List<String> roles = jdbc.sql("""
                SELECT r.role_code FROM sys_role r
                JOIN sys_user_role ur ON ur.role_id = r.id
                WHERE ur.user_id = :id AND r.status = 1 AND r.deleted = 0 ORDER BY r.sort_order
                """).param("id", row.id()).query(String.class).list();
        List<String> permissions = jdbc.sql("""
                SELECT DISTINCT p.permission_code FROM sys_permission p
                JOIN sys_role_permission rp ON rp.permission_id = p.id
                JOIN sys_user_role ur ON ur.role_id = rp.role_id
                JOIN sys_role r ON r.id=ur.role_id
                WHERE ur.user_id = :id AND r.status=1 AND r.deleted=0
                AND p.status = 1 AND p.deleted = 0 ORDER BY p.permission_code
                """).param("id", row.id()).query(String.class).list();
        Set<Long> assignedScopes = new LinkedHashSet<>(jdbc.sql("""
                SELECT uds.data_scope_id FROM sys_user_data_scope uds
                JOIN sys_data_scope ds ON ds.id=uds.data_scope_id AND ds.active=TRUE
                WHERE uds.user_id = :id ORDER BY uds.data_scope_id
                """).param("id", row.id()).query(Long.class).list());
        Set<Long> scopes=new LinkedHashSet<>(assignedScopes);
        scopes.addAll(jdbc.sql("""
                SELECT DISTINCT s.resource_id FROM sys_share_rule s
                JOIN sys_data_scope ds ON ds.id=s.resource_id AND ds.active=TRUE
                WHERE s.resource_type='DATA_SCOPE' AND s.status='ACTIVE' AND 'READ'=ANY(s.operations)
                  AND s.valid_from<=now() AND (s.valid_to IS NULL OR s.valid_to>now())
                  AND ((s.grantee_type='USER' AND s.grantee_id=:id)
                    OR (s.grantee_type='ROLE' AND EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0 WHERE ur.user_id=:id AND ur.role_id=s.grantee_id))
                    OR (s.grantee_type='DEPARTMENT' AND EXISTS(SELECT 1 FROM sys_user u JOIN sys_department d ON d.id=u.department_id AND d.status=1 AND d.deleted=0 WHERE u.id=:id AND u.department_id=s.grantee_id)))
                ORDER BY s.resource_id
                """).param("id",row.id()).query(Long.class).list());
        return new UserPrincipal(row.id(), row.username(), row.realName(), row.password(), row.status() == 1, row.mustChangePassword(),
                roles, permissions, assignedScopes, scopes);
    }

    private record UserRow(long id, String username, String realName, String password, int status, boolean mustChangePassword) {}
}
