package com.miguelcebing.tallerproxy.logging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SensitiveDataMasker {

    private static final String MASK = "***";
    private static final List<String> SENSITIVE_KEYS = List.of("password", "token", "secret", "card", "cvv");

    public List<Object> maskAll(Object[] params) {
        List<Object> masked = new ArrayList<>();
        if (params == null) {
            return masked;
        }
        for (Object param : params) {
            masked.add(mask(param));
        }
        return List.copyOf(masked);
    }

    public Object mask(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return maskMap(map);
        }
        if (value instanceof Collection<?> collection) {
            return maskCollection(collection);
        }
        if (value.getClass().isArray()) {
            return maskArray(value);
        }
        if (isSimpleValue(value)) {
            return value;
        }
        return String.valueOf(value);
    }

    private Map<Object, Object> maskMap(Map<?, ?> map) {
        Map<Object, Object> copy = new LinkedHashMap<>();
        map.forEach((key, value) -> copy.put(key, isSensitive(key) ? MASK : mask(value)));
        return copy;
    }

    private List<Object> maskCollection(Collection<?> collection) {
        List<Object> copy = new ArrayList<>();
        collection.forEach(item -> copy.add(mask(item)));
        return copy;
    }

    private List<Object> maskArray(Object array) {
        int length = java.lang.reflect.Array.getLength(array);
        List<Object> copy = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            copy.add(mask(java.lang.reflect.Array.get(array, index)));
        }
        return copy;
    }

    private boolean isSensitive(Object key) {
        String normalized = String.valueOf(key).toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Instant
                || value instanceof Enum<?>;
    }
}
