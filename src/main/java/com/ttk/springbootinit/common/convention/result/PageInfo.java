package com.ttk.springbootinit.common.convention.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应
 *
 * @author Rangsh
 */
@Data
public class PageInfo<T> {

    private Long current = 1L;

    private Long size = 10L;

    private Long total = 0L;

    private Long pages = 0L;

    private List<T> records = Collections.emptyList();

    public PageInfo() {
    }

    public PageInfo(Long current, Long size) {
        this.current = current;
        this.size = size;
    }

    public PageInfo(Long current, Long size, Long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records == null ? Collections.emptyList() : records;
        if (size != null && size > 0 && total != null) {
            this.pages = total % size == 0 ? total / size : total / size + 1;
        }
    }

    public static <T> PageInfo<T> of(Page<T> page) {
        return new PageInfo<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public static <T> PageInfo<T> empty() {
        return new PageInfo<>();
    }

    public static <T> PageInfo<T> of(long current, long size) {
        return new PageInfo<>(current, size);
    }
}
