package com.storephone.dto;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {
    private final List<T> items;
    private final long totalElements;
    private final int page;
    private final int pageSize;

    public PageResult(List<T> items, long totalElements, int page, int pageSize) {
        this.items = items == null ? Collections.emptyList() : List.copyOf(items);
        this.totalElements = Math.max(0, totalElements);
        this.page = Math.max(1, page);
        this.pageSize = Math.max(1, pageSize);
    }

    public List<T> getItems() {
        return items;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        if (totalElements == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalElements / pageSize);
    }
}
