package com.ttk.springbootinit.config.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer;
import com.ttk.springbootinit.common.constant.DateConstant;
import com.ttk.springbootinit.common.util.JsonUtils;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.TimeZone;

/**
 * Jackson 全局配置：Long 精度、Java 8 时间格式、时区
 * <p>
 * jsr310 由 spring-boot-starter-json 传递引入，无需在 pom 中重复声明。
 * Jackson 版本跟随 Spring Boot BOM，不要单独锁版本以免冲突。
 *
 * @author Rangsh
 */
@Configuration
public class JsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            builder.featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    SerializationFeature.FAIL_ON_EMPTY_BEANS,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            );

            // Long -> String，避免前端 JS 精度丢失
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // Java 8 时间统一格式
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DateConstant.DATE_TIME_FORMATTER));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DateConstant.DATE_TIME_FORMATTER));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(DateConstant.DATE_FORMATTER));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(DateConstant.DATE_FORMATTER));
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(DateConstant.TIME_FORMATTER));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(DateConstant.TIME_FORMATTER));
            builder.serializerByType(YearMonth.class, new YearMonthSerializer(DateConstant.YEAR_MONTH_FORMATTER));
            builder.deserializerByType(YearMonth.class, new YearMonthDeserializer(DateConstant.YEAR_MONTH_FORMATTER));
        };
    }

    /**
     * 将 Spring 管理的 ObjectMapper 同步给 JsonUtils
     */
    @Bean
    public ApplicationRunner jsonUtilsInitializer(ObjectMapper objectMapper) {
        return args -> JsonUtils.init(objectMapper);
    }
}
