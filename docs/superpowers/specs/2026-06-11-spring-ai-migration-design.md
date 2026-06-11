# Spring AI Alibaba 迁移设计文档

> 日期：2026-06-11
> 状态：已批准
> 关联：Xinki Portfolio 后端 AI 层重构

## 1. 目标

将项目中所有直接调用阿里百炼 API 的代码（java.net.http.HttpClient）替换为 Spring AI Alibaba 框架，同时保留自建的 RAG 基础设施（向量缓存、文档分块、内容索引）。

## 2. 方案选择

采用 **方案 3：Spring AI Alibaba + 保留自建 RAG 基础设施**。

- Chat / Streaming / Embedding → `spring-ai-alibaba-starter`（ChatClient + EmbeddingModel）
- RAG 基础设施（VectorCacheService + DocumentChunkService + ContentIndexService）→ **保留不变**
- Spring Boot 2.7.18 → 3.4.x（连带 javax→jakarta 迁移）

## 3. 依赖变更

### 3.1 Spring Boot 版本

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>  →  <version>3.4.3</version>
</parent>
```

### 3.2 新增依赖

```xml
<dependencyManagement>
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-bom</artifactId>
        <version>1.0.0-M6</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencyManagement>

<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
</dependency>
```

### 3.3 升级依赖

| 依赖 | 旧版本 | 新版本 | 原因 |
|------|--------|--------|------|
| mybatis-plus-boot-starter | 3.5.5 | 3.5.9 | Spring Boot 3 兼容性 |
| java-jwt | 4.4.0 | 4.4.0 | 不变，已验证兼容 |
| pdfbox | 2.0.30 | 3.0.3 | javax→jakarta 适配 |
| mysql-connector-j | (managed) | (managed) | Boot 3 自带新版 |

## 4. 配置变更

### 4.1 application.yml

删除整个 `bailian` 配置块，替换为：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${BAILIAN_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.3
          max-tokens: 4096
      embedding:
        options:
          model: text-embedding-v3
```

### 4.2 同步 deploy/application-prod.yml

同上。

## 5. 代码变更清单

### 5.1 删除

| 文件 | 原因 |
|------|------|
| `config/AIConfig.java` | Spring AI 自动配置替代 |
| `config/RedisConfig.java` | Spring Boot 3 自动配置 StringRedisTemplate，该文件导致冲突 |
| `service/EmbeddingService.java` | 用 Spring AI `EmbeddingModel` 替代 |

### 5.2 重写（AI 层）

| 文件 | 变更 |
|------|------|
| `service/impl/AIChatServiceImpl.java` | `HttpClient` + 手动 SSE → `ChatClient` + `Flux` |
| `service/impl/AIContentServiceImpl.java` | `HttpClient` → `ChatClient` |
| `service/impl/AdminAIServiceImpl.java` | `HttpClient` → `ChatClient` |

### 5.3 修改（javax → jakarta）

所有 Java 文件中的 `javax.*` 导入替换为 `jakarta.*`，涉及约 20 个文件：

| 文件 | 变更导入 |
|------|----------|
| `PortfolioApplication.java` | `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct` |
| `config/WebMvcConfig.java` | `javax.servlet.*` → `jakarta.servlet.*` |
| `config/JwtInterceptor.java` | `javax.servlet.*` → `jakarta.servlet.*` |
| `entity/*.java` (7 个) | `javax.persistence.*`（如有）→ `jakarta.persistence.*` |
| `controller/*.java` (8 个) | `javax.servlet.*`（如有）→ `jakarta.servlet.*` |
| `service/impl/VectorCacheService.java` | `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct` |

### 5.4 修改（Embedding 引用替换）

| 文件 | 变更 |
|------|------|
| `service/impl/AIChatServiceImpl.java` | `EmbeddingService` → `EmbeddingModel` |
| `service/ContentIndexService.java` | `EmbeddingService` → `EmbeddingModel` |
| `service/impl/AIContentServiceImpl.java` | 如有 EmbeddingService 引用需替换 |

### 5.5 不变

| 文件/模块 | 说明 |
|-----------|------|
| `VectorCacheService.java` | Redis 向量缓存（含降级） |
| `DocumentChunkService.java` | 文档分块逻辑 |
| `ContentIndexService.java`（除 Embedding 注入） | 内容自动索引 |
| `OssService.java` / `OssConfig.java` | OSS 上传 |
| `JwtUtil.java` | JWT 工具 |
| Mapper 层 | MyBatis-Plus 不变 |
| **前端代码（Vue 3）** | API 接口无变化 |

## 6. AIChatServiceImpl 重写后结构

```java
@Service
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final EmbeddingModel embeddingModel;
    private final RagConfig ragConfig;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ProjectMapper projectMapper;
    private final SkillMapper skillMapper;
    private final VectorCacheService vectorCacheService;

    // 非流式聊天
    public Map<String, Object> chat(String sessionId, String message) {
        // 1. RAG 检索（用 EmbeddingModel）
        String context = mergeContext(searchKnowledge(message), buildCatalog());
        // 2. ChatClient 调用
        String reply = chatClientBuilder.build()
            .prompt()
            .system(buildSystemPrompt(context))
            .user(message)
            .call()
            .content();
        // 3. 保存历史
        ...
    }

    // 流式聊天
    public SseEmitter chatStream(String sessionId, String message) {
        ...
        Flux<String> flux = chatClientBuilder.build()
            .prompt()
            .system(buildSystemPrompt(context))
            .user(message)
            .stream()
            .content();
        // Flux<String> → SseEmitter
        ...
    }

    // RAG 检索（embeddingModel.embed()）
    private String searchKnowledge(String query) {
        float[] queryVec = embeddingModel.embed(query);
        ...
    }
}
```

## 7. Docker 变更

| 文件 | 变更 |
|------|------|
| `Dockerfile.backend` | 基础镜像 `eclipse-temurin:17-jre-alpine` → `eclipse-temurin:21-jre-alpine` |
| `deploy/settings-docker.xml` | 不变 |
| `deploy/application-prod.yml` | 同步 AI 配置（bailian.* → spring.ai.dashscope.*） |
| `docker-compose.yml` | 不变 |
| `.env.example` | 不变（BAILIAN_API_KEY 仍然需要） |

## 8. 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| javax→jakarta 全局替换遗漏 | 中 | 编译阶段会发现，mvn compile 验证 |
| spring-ai-alibaba M6 API 变更 | 低 | M6 接近 RC，锁定版本号 |
| MyBatis-Plus 兼容性 | 低 | 3.5.9 已完整支持 Spring Boot 3 |
| PDFBox 3.x API 变更 | 低 | API 变化小，编译期修复 |
| SSE 行为差异 | 低 | 前端不变，后端产生相同 SSE 事件格式 |

## 9. 验收标准

- [ ] `mvn compile` 通过（Spring Boot 3.4.x）
- [ ] `/api/ai/chat` 非流式对话正常（RAG 检索 + 回复）
- [ ] `/api/ai/chat/stream` 流式 SSE 逐字输出正常
- [ ] `/api/ai/chat/generate-content` AI 生成作品内容正常
- [ ] `/api/admin/ai/analyze-document` 文档分析正常
- [ ] `/api/admin/knowledge/import` 知识库导入 + 向量化正常
- [ ] `/api/admin/projects/reindex` 全量重建索引正常
- [ ] 前端所有页面功能无回归
- [ ] Docker 构建成功（`docker compose build`）