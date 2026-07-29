package com.ttk.springbootinit.controller;

import com.ttk.springbootinit.common.convention.result.Result;
import com.ttk.springbootinit.common.convention.result.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模版连通性测试接口（无需登录）
 *
 * @author Rangsh
 */
@Tag(name = "测试接口")
@RestController
@RequestMapping("/test")
public class TestController {

    @Operation(summary = "健康探测", description = "用于验证服务是否启动成功，返回 pong")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Results.success("pong");
    }
}
