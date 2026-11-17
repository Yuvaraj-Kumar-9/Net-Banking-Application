package com.hfdcbank.dto;

import java.util.List;

/** Generic pagination envelope used by the transaction history endpoint. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
