package com.nexuspms.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** API Design S2.2 offset-pagination envelope: { data, pagination: { page, size, totalElements, hasMore } }. */
public record PageResponse<T>(List<T> data, Pagination pagination) {

    public record Pagination(int page, int size, long totalElements, boolean hasMore) {
    }

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        List<T> mapped = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(mapped, new Pagination(
                page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext()));
    }
}
