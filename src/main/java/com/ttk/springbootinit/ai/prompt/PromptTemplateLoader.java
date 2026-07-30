package com.ttk.springbootinit.ai.prompt;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 模版加载器。
 * <p>
 * 启动时扫描 {@code classpath:prompt/*.md}，解析 YAML front matter + body。
 * <p>
 * 文件格式：
 * <pre>{@code
 * ---
 * name: code-review
 * version: 1.0.0
 * description: 代码审查提示词
 * model: gpt-4o
 * temperature: 0.3
 * ---
 * 请审查以下代码：
 * {{code}}
 * 关注点：{{focus}}
 * }</pre>
 *
 * @author Rangsh
 */
@Component
public class PromptTemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateLoader.class);

    private static final String YAML_DELIMITER = "---";
    private static final String SCAN_PATTERN = "classpath:prompt/*.md";

    private final ConcurrentHashMap<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    public PromptTemplateLoader() {
        loadTemplates();
    }

    /**
     * 扫描并加载所有 prompt 模版。
     */
    @SuppressWarnings("unchecked")
    private void loadTemplates() {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(SCAN_PATTERN);
            for (Resource resource : resources) {
                try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    String fullContent = IoUtil.read(reader);
                    PromptTemplate template = parseTemplate(fullContent);
                    if (template != null) {
                        String key = cacheKey(template.getName(), template.getVersion());
                        cache.put(key, template);
                        log.info("Prompt 模版已加载: {} (v{})", template.getName(), template.getVersion());
                    }
                } catch (Exception e) {
                    log.warn("Prompt 模版解析失败: {} - {}", resource.getFilename(), e.getMessage());
                }
            }
            log.info("共加载 {} 个 Prompt 模版", cache.size());
        } catch (Exception e) {
            log.warn("Prompt 模版扫描失败: {}", e.getMessage());
        }
    }

    /**
     * 按名称获取最新版本模版。
     */
    public PromptTemplate load(String name) {
        return cachedTemplates().stream()
                .filter(t -> t.getName().equals(name))
                .max(Comparator.comparing(PromptTemplate::getVersion))
                .orElse(null);
    }

    /**
     * 按名称 + 版本获取模版。
     */
    public PromptTemplate load(String name, String version) {
        return cache.get(cacheKey(name, version));
    }

    /**
     * 获取已加载的所有模版。
     */
    public List<PromptTemplate> cachedTemplates() {
        return new ArrayList<>(cache.values());
    }

    /**
     * 手动注册/更新模版（用于运行时动态添加）。
     */
    public void register(PromptTemplate template) {
        String key = cacheKey(template.getName(), template.getVersion());
        cache.put(key, template);
        log.info("Prompt 模版已注册: {} (v{})", template.getName(), template.getVersion());
    }

    /**
     * 重新扫描。
     */
    public void reload() {
        cache.clear();
        loadTemplates();
    }

    @SuppressWarnings("unchecked")
    private PromptTemplate parseTemplate(String fullContent) {
        if (StrUtil.isBlank(fullContent)) {
            return null;
        }

        String trimmed = fullContent.trim();
        if (!trimmed.startsWith(YAML_DELIMITER)) {
            // 无 front matter，整篇为 content
            Set<String> vars = PromptTemplate.extractVariables(trimmed);
            return PromptTemplate.builder()
                    .name("unnamed")
                    .version("0.0.1")
                    .content(trimmed)
                    .variables(vars)
                    .build();
        }

        // 找到第二个 "---" 分隔符
        int endIdx = trimmed.indexOf(YAML_DELIMITER, 3);
        if (endIdx < 0) {
            return null;
        }

        String yamlStr = trimmed.substring(3, endIdx).trim();
        String body = trimmed.substring(endIdx + 3).trim();

        Map<String, Object> meta = new Yaml().load(yamlStr);
        String name = String.valueOf(meta.getOrDefault("name", "unnamed"));
        String version = String.valueOf(meta.getOrDefault("version", "0.0.1"));
        String description = (String) meta.get("description");
        String model = (String) meta.get("model");
        Double temperature = meta.get("temperature") instanceof Number n ? n.doubleValue() : null;
        Integer maxTokens = meta.get("max_tokens") instanceof Number n ? n.intValue() : null;

        Set<String> vars = PromptTemplate.extractVariables(body);

        return PromptTemplate.builder()
                .name(name)
                .version(version)
                .description(description)
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .content(body)
                .variables(vars)
                .build();
    }

    private String cacheKey(String name, String version) {
        return name + "@" + version;
    }
}
