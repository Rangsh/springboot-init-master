# SpringBoot Init Master

基于 **Spring Boot 3 + JDK 17** 的 Java 后端开发模版（单体架构）。

当前仓库处于**骨架阶段**：已就绪工程约定、通用基建与扩展占位，业务模块由使用者按自身需求自行扩展。

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8-blue?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-red?logo=redis&logoColor=white)](https://redis.io/)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.9-blue)](https://baomidou.com/)
[![Sa-Token](https://img.shields.io/badge/Sa--Token-1.39.0-brightgreen)](https://sa-token.cc/)
[![Druid](https://img.shields.io/badge/Druid-1.2.25-orange)](https://github.com/alibaba/druid)
[![Knife4j](https://img.shields.io/badge/Knife4j-4.5.0-green)](https://doc.xiaominfo.com/)
[![OpenAI](https://img.shields.io/badge/LLM-OpenAI%20%7C%20Claude%20%7C%20DeepSeek-blueviolet)](https://platform.openai.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

## 模版定位

- **AI 原生**：内置 LLM 客户端抽象层，策略模式统一 OpenAI / Claude / DeepSeek / 本地模型，开箱即用的 SSE 流式 + Agent 工具调用
- **约定型骨架**：统一响应、错误码、全局异常、鉴权拦截、分页与基础配置先给齐
- **不绑定具体业务**：不内置用户/订单等示例域，避免和你的业务模型冲突
- **可扩展**：预留 MCP、Skill、Docker、SQL 等目录与依赖，按需启用

## 已具备能力

- Spring Boot 3.4 / JDK 17 单体工程 + Maven Wrapper
- 统一返回体 `Result`、分页 `PageInfo` / `PageRequest`、A/B/C 错误码与三类业务异常
- 全局异常处理（参数校验、业务异常、Sa-Token 未登录/无权限）
- 请求追踪 `traceId`（MDC + 响应头 `X-Trace-Id`）
- Sa-Token + Redis 会话，接口文档 Knife4j（OpenAPI3）
- MyBatis-Plus（分页、逻辑删除、字段自动填充）+ Druid 连接池（本地可开监控页）
- Redis / Redisson、腾讯云 COS（默认关闭，开关启用）
- **LLM 客户端抽象层**（`ai/` 模块）：策略模式统一 4 个 Provider（OpenAI / Claude / DeepSeek / Ollama 本地模型）
- SSE 流式聊天 + ReAct Agent 工具调用循环
- Prompt 模板管理（YAML front matter + `{{变量}}` 引擎）
- 多环境配置：`dev` / `prod`
- 扩展占位：`mcp-server/`、`skills/`、`sql/`、Docker 相关文件

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 运行时 | Java | 17 |
| 框架 | Spring Boot | 3.4.4 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL 8（`mysql-connector-j`） | Boot 3.4.4 管理 |
| 连接池 | Alibaba Druid（`druid-spring-boot-3-starter`） | 1.2.25 |
| 缓存 / 锁 | Redis + Lettuce 连接池（`commons-pool2`） | Boot 管理 / 2.12.1 |
| 缓存 / 锁 | Redisson | 3.27.2 |
| 鉴权 | Sa-Token + Redis Jackson | 1.39.0 |
| 文档 | Knife4j UI + springdoc（兼容 Boot 3.4） | 4.5.0 / 2.8.17 |
| Web 能力 | Validation / AOP / Actuator | Boot 3.4.4 管理 |
| JSON | Jackson（含 jsr310，Boot BOM 管理，当前 2.18.3） | Boot 3.4.4 管理 |
| 工具 | Hutool | 5.8.27 |
| 工具 | Lombok | 1.18.36（Boot 管理） |
| HTTP | OkHttp | 4.12.0 |
| 对象存储 | 腾讯云 COS（`cos.enabled=true` 时启用） | 5.6.227 |
| LLM 客户端 | OpenAI / Claude / DeepSeek / 本地模型（OkHttp + SSE） | — |
| 扩展 | MCP SDK | 1.1.2 |

## 目录结构

```text
springboot-init-master/
├── pom.xml
├── mvnw / .mvn/                 # Maven Wrapper
├── lombok.config
├── LICENSE / NOTICE             # Apache-2.0 与署名
├── sql/                         # 建表脚本（按需补充）
├── mcp-server/                  # 独立 MCP Server 预留（暂不参与构建）
├── skills/                      # Agent Skill 预留（无运行时依赖）
├── Dockerfile / docker-compose  # 部署预留（当前为空壳占位）
└── src/main/
    ├── java/com/ttk/springbootinit/
    │   ├── MainApplication.java
    │   ├── common/
    │   │   ├── convention/      # result / errorcode / exception
    │   │   ├── constant/
    │   │   ├── model/           # BaseDO
    │   │   ├── util/
    │   │   └── web/             # 全局异常、TraceId
    │   ├── config/
    │   │   ├── web/             # CORS、Jackson、OpenAPI、Knife4j 配置端点
    │   │   ├── database/        # MP 分页与自动填充
    │   │   ├── auth/            # Sa-Token
    │   │   └── oss/             # COS
    │   ├── controller/          # 接口（含测试 ping）
    │   ├── ai/                   # LLM 客户端抽象层
    │   │   ├── provider/         # 策略模式：LlmProvider + 4 个实现
    │   │   ├── model/            # ChatRequest / ChatResponse / Tool 等 DTO
    │   │   ├── config/           # LlmProperties + 条件装配
    │   │   ├── controller/       # /ai/chat、/ai/agent 等接口
    │   │   ├── tool/             # 工具注册表（ToolRegistry）
    │   │   ├── prompt/           # Prompt 模板加载与渲染
    │   │   ├── streaming/        # SSE 桥接（SseEmitterBridge）
    │   │   └── agent/            # ReAct Agent 循环
    │   └── mapper/
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        ├── banner.txt
        ├── mapper/
        └── prompt/              # 可选 prompt 文本占位
```

业务代码建议按领域分包（如 `user`、`order`），与 `common` / `config` 并列，保持单体清晰边界。

## 快速开始

### 环境

- JDK 17+
- Maven 3.9+（也可直接使用项目自带 `./mvnw`）
- MySQL 8、Redis（按本地配置启动）

### 配置

默认即本地开发配置，直接改 `src/main/resources/application.yml`：

- MySQL：`127.0.0.1:3306/springboot_init`，账号 `root` / `123456`
- Redis：`127.0.0.1:6379`（默认无密码；若本机开了 `requirepass`，改 `spring.data.redis.password`）
- 端口：`8101`，上下文：`/api`
- 默认 profile：`dev`（会打印 SQL）

部署时使用 `--spring.profiles.active=prod`，敏感项走环境变量（见 `application-prod.yml`）。

### 启动

```bash
./mvnw spring-boot:run
# 或 IDE 运行 MainApplication
```

### 常用地址

| 用途 | 地址 |
|------|------|
| Knife4j 文档 | http://localhost:8101/api/doc.html |
| 测试接口 | http://localhost:8101/api/test/ping |
| Druid 监控 | http://localhost:8101/api/druid/index.html（`admin` / `admin`） |
| 健康检查 | http://localhost:8101/api/actuator/health |
| AI 同步聊天 | http://localhost:8101/api/ai/chat（需 `llm.enabled=true`） |
| AI 流式聊天 | http://localhost:8101/api/ai/chat/stream（SSE） |
| AI Agent | http://localhost:8101/api/ai/agent（工具调用） |

> 默认开启 Sa-Token 登录校验；Knife4j、Actuator、Druid 监控、`/test/**`，以及预留的 `/user/login`、`/user/register` 已放行。本地文档地址见上；生产默认关闭 springdoc。

## 使用约定（简要）

- 接口返回：`Results.success(data)` / 抛出 `ClientException`、`ServiceException`、`RemoteException`
- 实体公共字段：继承 `BaseDO`（`createTime` / `updateTime` / `isDelete`）
- 分页：入参可用 `PageRequest`，出参可用 `PageInfo.of(page)`
- 追踪：日志带 `traceId`，响应头 `X-Trace-Id`，`Result.requestId` 自动填充
- 时间：统一 `LocalDateTime` 等，JSON 格式为 `yyyy-MM-dd HH:mm:ss`（见 `DateConstant` / `JsonConfig`）
- COS：设置 `cos.enabled=true` 并填写密钥后再使用
- LLM：在 `application.yml` 中设置 `llm.enabled=true`，并启用对应 Provider（`api-key` 建议用环境变量注入）
  - 同步调用：`provider.chat(request)` → `ChatResponse`
  - 流式调用：`provider.chatStream(request, callback)` → SSE 推送
  - 工具注册：`toolRegistry.register(definition, handler)` → Agent 自动调用
  - Prompt 模板：在 `prompt/*.md` 中添加 YAML front matter + `{{变量}}`

## 后续规划

模版会持续完善工程能力（配置、约定与扩展位）。具体业务实现请按项目需要自行添加，本仓库不强制统一业务模型。

## License

Copyright 2026 Rangsh

本项目基于 [Apache License 2.0](./LICENSE) 开源，署名信息见 [NOTICE](./NOTICE)。
