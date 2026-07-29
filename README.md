# SpringBoot Init Master

基于 **Spring Boot 3 + JDK 17** 的 Java 后端开发模版（单体架构）。

当前仓库处于**骨架阶段**：已就绪工程约定、通用基建与扩展占位，业务模块由使用者按自身需求自行扩展。

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

## 模版定位

- **约定型骨架**：统一响应、错误码、全局异常、鉴权拦截、分页与基础配置先给齐
- **不绑定具体业务**：不内置用户/订单等示例域，避免和你的业务模型冲突
- **可扩展**：预留 MCP、Skill、Docker、SQL 等目录与依赖，按需启用

## 已具备能力

- Spring Boot 3.4 / JDK 17 单体工程 + Maven Wrapper
- 统一返回体 `Result`、分页 `PageInfo` / `PageRequest`、A/B/C 错误码与三类业务异常
- 全局异常处理（参数校验、业务异常、Sa-Token 未登录/无权限）
- 请求追踪 `traceId`（MDC + 响应头 `X-Trace-Id`）
- Sa-Token + Redis 会话，接口文档 Knife4j（OpenAPI3）
- MyBatis-Plus（分页、逻辑删除、字段自动填充）
- Redis / Redisson、腾讯云 COS（默认关闭，开关启用）
- 多环境配置：`dev` / `prod`
- 扩展占位：`mcp-server/`、`skills/`、`sql/`、Docker 相关文件

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 运行时 | Java | 17 |
| 框架 | Spring Boot | 3.4.4 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL 8（`mysql-connector-j`） | Boot 管理 |
| 缓存 / 锁 | Redis、Lettuce 连接池、Redisson | 3.27.2 |
| 鉴权 | Sa-Token + Redis Jackson | 1.39.0 |
| 文档 | Knife4j OpenAPI3 Jakarta | 4.5.0 |
| 工具 | Hutool、Lombok、OkHttp、Validation、AOP、Actuator | — |
| 对象存储 | 腾讯云 COS（`cos.enabled=true` 时启用） | 5.6.227 |
| 扩展 | MCP SDK | 1.1.2 |

## 目录结构

```text
springboot-init-master/
├── pom.xml
├── mvnw / .mvn/                 # Maven Wrapper
├── lombok.config
├── LICENSE                      # Apache-2.0
├── sql/                         # 建表脚本（按需补充）
├── mcp-server/                  # 独立 MCP Server 预留（暂不参与构建）
├── skills/                      # Agent Skill 预留（无运行时依赖）
├── Dockerfile / docker-compose  # 部署预留
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
    │   │   ├── web/             # CORS、Jackson
    │   │   ├── database/        # MP 分页与自动填充
    │   │   ├── auth/            # Sa-Token
    │   │   └── oss/             # COS
    │   └── mapper/
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        ├── mapper/
        └── prompt/
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
- Redis：`127.0.0.1:6379`
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
| 接口文档 | http://localhost:8101/api/doc.html |
| OpenAPI | http://localhost:8101/api/v3/api-docs |
| 健康检查 | http://localhost:8101/api/actuator/health |

> 默认开启 Sa-Token 登录校验；文档、Actuator、以及预留的 `/user/login`、`/user/register` 已放行。业务登录接口实现后即可对接。

## 使用约定（简要）

- 接口返回：`Results.success(data)` / 抛出 `ClientException`、`ServiceException`、`RemoteException`
- 实体公共字段：继承 `BaseDO`（`createTime` / `updateTime` / `isDelete`）
- 分页：入参可用 `PageRequest`，出参可用 `PageInfo.of(page)`
- 追踪：日志带 `traceId`，响应头 `X-Trace-Id`，`Result.requestId` 自动填充
- COS：设置 `cos.enabled=true` 并填写密钥后再使用

## 后续规划

模版会持续完善工程能力（配置、约定与扩展位）。具体业务实现请按项目需要自行添加，本仓库不强制统一业务模型。

## License

[Apache License 2.0](./LICENSE)
