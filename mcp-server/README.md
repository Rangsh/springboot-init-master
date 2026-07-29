# mcp-server（预留）

MCP Server 是**独立 Maven 模块**（可单独部署），不是主应用里的普通包。

当前主工程已引入 MCP SDK（`mcp` / `mcp-json-jackson2`），可先在主工程内试用 Client。

启用独立 Server 时建议：

1. 将本仓库改为多模块（parent `pom` + `modules`）
2. 在此目录补齐 `pom.xml` 与 `src/main/java`
3. 暴露 `/mcp` 端点，主应用通过配置连接（自行约定 MCP Server 地址与鉴权）

现阶段仅保留目录占位，不参与构建。
