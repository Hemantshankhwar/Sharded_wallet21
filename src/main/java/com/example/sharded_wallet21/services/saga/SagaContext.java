package com.example.sharded_wallet21.services.saga;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Builder
public class SagaContext {

    private Map<String, Object> data = new HashMap<>();

    public SagaContext(Map<String, Object> data) {
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Long getLong(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = get(key);
        return value == null ? null : new BigDecimal(value.toString());
    }

    public String getString(String key) {
        Object value = get(key);
        return value == null ? null : value.toString();
    }
}
