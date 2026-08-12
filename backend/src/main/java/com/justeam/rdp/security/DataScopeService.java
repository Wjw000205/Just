package com.justeam.rdp.security;

import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.sharing.SharingService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DataScopeService {
    private final SharingService sharing;private final JdbcClient jdbc;public DataScopeService(SharingService sharing,JdbcClient jdbc){this.sharing=sharing;this.jdbc=jdbc;}
    public void require(long scopeId) {
        UserPrincipal user = CurrentUser.require();
        jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock_shared(4205)) locked").query(Long.class).single();
        long active=jdbc.sql("SELECT count(*) FROM sys_data_scope WHERE id=:id AND active=TRUE AND deleted=0").param("id",scopeId).query(Long.class).single();
        if(active==0)throw BusinessException.badRequest("数据域不存在或已停用");
        if (!user.admin() && !user.assignedScopes().contains(scopeId)) {
            throw BusinessException.forbidden("无权访问该数据域");
        }
    }

    public void requireRead(long scopeId){if(!canAccess(scopeId))throw BusinessException.forbidden("无权访问该数据域");}

    public void requireOperation(long scopeId,String operation){UserPrincipal user=CurrentUser.require();if(user.admin()||user.assignedScopes().contains(scopeId))return;if(!sharing.hasScopeAccess(scopeId,operation))throw BusinessException.forbidden("共享规则未授权该操作");}

    public boolean canAccess(long scopeId) {
        UserPrincipal user = CurrentUser.require();
        return user.admin() || user.dataScopes().contains(scopeId);
    }

    public Set<Long> currentScopes() {
        return CurrentUser.require().dataScopes();
    }

    public boolean canWriteAccess(long scopeId){UserPrincipal user=CurrentUser.require();return user.admin()||user.assignedScopes().contains(scopeId);}
}
