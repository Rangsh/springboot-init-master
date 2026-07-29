package com.ttk.springbootinit.common.convention.result;

import lombok.Data;

/**
 * 分页请求
 */
@Data
public class PageRequest {

    /** 当前页号 */
    private long current = 1;

    /** 页面大小 */
    private long pageSize = 10;

    /** 排序字段 */
    private String sortField;

    /** 排序顺序：asc / desc */
    private String sortOrder = "asc";
}
