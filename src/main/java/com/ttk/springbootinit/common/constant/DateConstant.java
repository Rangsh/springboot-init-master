package com.ttk.springbootinit.common.constant;

import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式常量
 *
 * @author Rangsh
 */
public final class DateConstant {

    private DateConstant() {
    }

    /** 年-月-日 时:分:秒 */
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /** 年-月-日 */
    public static final String PATTERN_DATE = "yyyy-MM-dd";

    /** 时:分:秒 */
    public static final String PATTERN_TIME = "HH:mm:ss";

    /** 年-月 */
    public static final String PATTERN_YEAR_MONTH = "yyyy-MM";

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATETIME);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATE);

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_TIME);

    public static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_YEAR_MONTH);
}
