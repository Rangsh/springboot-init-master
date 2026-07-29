package com.ttk.springbootinit.config.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 补齐 Knife4j 所需的 swagger-config（替代 springdoc-ui，兼容 Boot 3.4）。
 *
 * @author Rangsh
 */
@RestController
public class Knife4jSwaggerConfigEndpoint {

    @GetMapping("/v3/api-docs/swagger-config")
    public Map<String, Object> swaggerConfig(HttpServletRequest request) {
        String contextPath = request.getContextPath();

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("configUrl", contextPath + "/v3/api-docs/swagger-config");
        config.put("url", contextPath + "/v3/api-docs");
        config.put("urls", List.of(Map.of(
                "url", contextPath + "/v3/api-docs",
                "name", "default"
        )));
        config.put("validatorUrl", "");
        return config;
    }
}
