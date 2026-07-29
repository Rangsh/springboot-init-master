package com.ttk.springbootinit.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * SQL 相关工具
 */
public final class SqlUtils {

    private SqlUtils() {
    }

    /**
     * 校验排序字段是否合法（防止 SQL 注入）
     */
    public static boolean validSortField(String sortField) {
        if (StrUtil.isBlank(sortField)) {
            return false;
        }
        return !StrUtil.containsAny(sortField, "=", "(", ")", " ");
    }
}
