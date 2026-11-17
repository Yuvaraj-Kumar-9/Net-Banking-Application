package com.hfdcbank.util;

import org.springframework.jdbc.support.KeyHolder;

import java.util.Map;

/**
 * Reads an auto-increment value from a {@link KeyHolder} in a way that works
 * with both MySQL 8 and the H2 database used by the tests.
 *
 * <p>MySQL returns a single "GENERATED_KEY" entry, while H2 returns every
 * generated/defaulted column (for example the id and created_at). Looking up
 * the known id column first avoids the "multiple keys" exception that
 * {@code keyHolder.getKey()} throws in that case.</p>
 */
public final class GeneratedKeys {

    private GeneratedKeys() {
    }

    public static long require(KeyHolder keyHolder, String idColumn, String errorMessage) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException(errorMessage);
        }

        Object preferred = keys.get(idColumn);
        if (preferred instanceof Number number) {
            return number.longValue();
        }

        // MySQL labels its only generated key "GENERATED_KEY".
        if (keys.size() == 1) {
            Object only = keys.values().iterator().next();
            if (only instanceof Number number) {
                return number.longValue();
            }
        }

        // Last resort: the first numeric value in the returned row.
        for (Object value : keys.values()) {
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        throw new IllegalStateException(errorMessage);
    }
}
