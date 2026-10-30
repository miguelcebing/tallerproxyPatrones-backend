package com.miguelcebing.tallerproxy.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void shouldMaskSensitiveKeysRecursively() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "miguel");
        payload.put("password", "super-secret");
        payload.put("nested", Map.of("cardNumber", "4111111111111111", "cvv", "123"));
        payload.put("items", List.of(Map.of("token", "abc")));

        Map<String, Object> masked = (Map<String, Object>) masker.mask(payload);

        assertThat(masked.get("username")).isEqualTo("miguel");
        assertThat(masked.get("password")).isEqualTo("***");
        Map<String, Object> nested = (Map<String, Object>) masked.get("nested");
        assertThat(nested.get("cardNumber")).isEqualTo("***");
        assertThat(nested.get("cvv")).isEqualTo("***");
        List<Object> items = (List<Object>) masked.get("items");
        Map<String, Object> firstItem = (Map<String, Object>) items.getFirst();
        assertThat(firstItem.get("token")).isEqualTo("***");
    }

    @Test
    void shouldNotMutateOriginalPayload_whenMasking() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("password", "super-secret");
        List<Object> items = new ArrayList<>();
        items.add(Map.of("secret", "abc"));
        payload.put("items", items);

        masker.mask(payload);

        assertThat(payload.get("password")).isEqualTo("super-secret");
        List<Object> originalItems = (List<Object>) payload.get("items");
        Map<String, Object> firstItem = (Map<String, Object>) originalItems.getFirst();
        assertThat(firstItem.get("secret")).isEqualTo("abc");
    }

    @Test
    void shouldConvertArraysToList_whenMasking() {
        Object[] params = {new String[]{"a", "b"}};

        List<Object> masked = masker.maskAll(params);

        assertThat(masked).hasSize(1);
        List<Object> arrayAsList = (List<Object>) masked.getFirst();
        assertThat(arrayAsList).containsExactly("a", "b");
    }
}
