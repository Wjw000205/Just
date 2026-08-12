package com.justeam.rdp.governance;

import com.justeam.rdp.common.BusinessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Resolves governed dictionary values for business services. */
@Service
public class DictionaryOptionService {
    private final JdbcClient jdbc;

    public DictionaryOptionService(JdbcClient jdbc){this.jdbc=jdbc;}

    public String requireEnabled(String dictionaryCode,String rawValue,String displayName){
        String value=rawValue==null?"":rawValue.trim().toUpperCase(Locale.ROOT);
        if(value.isEmpty())throw BusinessException.badRequest(displayName+"不能为空");
        jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock_shared(4206)) locked").query(Long.class).single();
        Long id=jdbc.sql("""
                SELECT i.id FROM gov_dictionary d
                JOIN gov_dictionary_item i ON i.dictionary_id=d.id
                WHERE d.dictionary_code=:code AND d.status=1 AND d.deleted=0
                  AND i.item_value=:value AND i.status=1 AND i.deleted=0
                FOR SHARE OF d,i
                """).param("code",dictionaryCode.trim().toUpperCase(Locale.ROOT)).param("value",value).query(Long.class).optional().orElse(null);
        if(id==null)throw BusinessException.badRequest(displayName+"不存在或已停用："+value);
        return value;
    }
}
