package com.lawfirm.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 分页结果封装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResult<T> of(Page<T> pageData) {
        return new PageResult<>(
                pageData.getContent(),
                pageData.getTotalElements(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalPages()
        );
    }

    public static <S, T> PageResult<T> of(Page<S> pageData, Function<S, T> mapper) {
        return new PageResult<>(
                pageData.getContent().stream().map(mapper).toList(),
                pageData.getTotalElements(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalPages()
        );
    }

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(items, total, page, size, totalPages);
    }
}
