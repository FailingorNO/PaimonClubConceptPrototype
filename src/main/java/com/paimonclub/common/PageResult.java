package com.paimonclub.common;

import java.util.Collections;
import java.util.List;

/**
 * 分页返回体。
 */
public class PageResult<T> {

    private long total;
    private int page;
    private int size;
    private int pages;
    private List<T> items;

    public PageResult() {
    }

    public PageResult(long total, int page, int size, List<T> items) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.items = items == null ? Collections.emptyList() : items;
        this.pages = size <= 0 ? 0 : (int) ((total + size - 1) / size);
    }

    public static <T> PageResult<T> of(long total, int page, int size, List<T> items) {
        return new PageResult<>(total, page, size, items);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(0, page, size, Collections.emptyList());
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
