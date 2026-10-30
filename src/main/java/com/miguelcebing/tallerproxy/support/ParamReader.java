package com.miguelcebing.tallerproxy.support;

import com.miguelcebing.tallerproxy.exception.InvalidRequestException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ParamReader {

    private ParamReader() {
    }

    public static Map<String, Object> asMap(Object... params) {
        if (params == null || params.length == 0 || params[0] == null) {
            return Map.of();
        }
        Object first = params[0];
        if (!(first instanceof Map<?, ?> rawMap)) {
            throw new InvalidRequestException("Request body must be a JSON object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    public static String requireString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new InvalidRequestException("Parameter '" + key + "' is required");
        }
        return String.valueOf(value);
    }

    public static int requirePositiveInt(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new InvalidRequestException("Parameter '" + key + "' is required");
        }
        int parsed = toInt(value, key);
        if (parsed <= 0) {
            throw new InvalidRequestException("Parameter '" + key + "' must be greater than zero");
        }
        return parsed;
    }

    public static double requirePositiveNumber(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new InvalidRequestException("Parameter '" + key + "' is required");
        }
        double parsed;
        try {
            parsed = Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Parameter '" + key + "' must be a number");
        }
        if (parsed <= 0) {
            throw new InvalidRequestException("Parameter '" + key + "' must be greater than zero");
        }
        return parsed;
    }

    public static String optionalString(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static int toInt(Object value, String key) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Parameter '" + key + "' must be an integer");
        }
    }
}
