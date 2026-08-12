package com.justeam.rdp.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonSupport {
    private final ObjectMapper mapper;

    public JsonSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON序列化失败", ex);
        }
    }

    public String canonical(Object value) {
        try {
            return mapper.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON规范化失败", ex);
        }
    }

    public Map<String, Object> map(String value) {
        try {
            return mapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw BusinessException.badRequest("JSON格式不正确");
        }
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
