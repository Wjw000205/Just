package com.justeam.rdp.dataset;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.function.Supplier;

@Component
public class DatasetMutationLock {
    private final DataSource dataSource;
    private final JdbcClient jdbc;

    public DatasetMutationLock(DataSource dataSource, JdbcClient jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    public void lockTransaction(long datasetId) {
        jdbc.sql("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended(:key,0))) locked")
                .param("key", key(datasetId)).query(Integer.class).single();
    }

    public <T> T withSessionLock(long datasetId, Supplier<T> action) {
        String key = key(datasetId);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement lock = connection.prepareStatement(
                     "SELECT 1 FROM (SELECT pg_advisory_lock(hashtextextended(?,0))) locked");
             PreparedStatement unlock = connection.prepareStatement(
                     "SELECT pg_advisory_unlock(hashtextextended(?,0))")) {
            lock.setString(1, key);
            try (var ignored = lock.executeQuery()) { ignored.next(); }
            try {
                return action.get();
            } finally {
                unlock.setString(1, key);
                try (var ignored = unlock.executeQuery()) { ignored.next(); }
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("获取数据集变更锁失败", ex);
        }
    }

    private String key(long datasetId) {
        return "rdp:dataset:" + datasetId;
    }
}
