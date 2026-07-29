# skills（预留）

`skills/` 是给 Cursor / Agent 用的**仓库级说明书**，不是 Java 依赖，不进 classpath。

每个 skill 建议结构：

```text
skills/
  your-skill-name/
    SKILL.md          # 必填：name / description / 使用说明
    references/       # 可选：领域说明、接口索引
    scripts/          # 可选：辅助脚本
```

需要时在此目录新增 skill，无需改 `pom.xml`。
