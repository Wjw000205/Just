package com.justeam.rdp.device;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.security.SecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

@Component
public class DeviceConfigEncryptionBackfill implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(DeviceConfigEncryptionBackfill.class);
    private final JdbcClient jdbc;private final SecretCipher cipher;private final AuditService audit;private final TransactionTemplate transactions;
    public DeviceConfigEncryptionBackfill(JdbcClient jdbc,SecretCipher cipher,AuditService audit,TransactionTemplate transactions){this.jdbc=jdbc;this.cipher=cipher;this.audit=audit;this.transactions=transactions;}

    @Override public void run(ApplicationArguments args){
        List<LegacyConfig> rows=jdbc.sql("SELECT id,connection_config::text AS config FROM device WHERE connection_config_ciphertext IS NULL AND connection_config<>'{}'::jsonb ORDER BY id")
                .query(LegacyConfig.class).list();
        for(LegacyConfig row:rows){try{transactions.executeWithoutResult(status->{int updated=jdbc.sql("UPDATE device SET connection_config_ciphertext=:ciphertext,connection_config='{}'::jsonb,updated_time=now() WHERE id=:id AND connection_config_ciphertext IS NULL")
                    .param("ciphertext",cipher.encrypt(row.config())).param("id",row.id()).update();if(updated==1)audit.recordAs(null,"system","ENCRYPT_BACKFILL","DEVICE","迁移历史设备连接配置为AES-GCM密文",Map.of("deviceId",row.id()));});}
            catch(Exception ex){log.error("设备 {} 的历史连接配置加密迁移失败，保留原值供下次启动重试",row.id(),ex);}}
        if(!rows.isEmpty())log.info("历史设备连接配置加密迁移扫描完成，共 {} 条",rows.size());
    }
    private record LegacyConfig(long id,String config){}
}
