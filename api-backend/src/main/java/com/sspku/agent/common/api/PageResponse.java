package com.sspku.agent.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long total;
    private int pageNo;
    private int pageSize;
    private List<T> records;

    public static <T> PageResponse<T> empty(int pageNo, int pageSize) {
        return new PageResponse<>(0L, pageNo, pageSize, Collections.emptyList());
    }

    public static <T> PageResponse<T> of(long total, int pageNo, int pageSize, List<T> records) {
        return new PageResponse<>(total, pageNo, pageSize, records);
    }
}

