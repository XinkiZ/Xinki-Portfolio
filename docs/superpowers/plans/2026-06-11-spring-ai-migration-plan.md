# Spring AI Alibaba 迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Xinki Portfolio 后端从手动 HttpClient 调用百炼 API 迁移到 Spring AI Alibaba 框架，同时升级 Spring Boot 2.7.18 → 3.4.x

**Architecture:** 核心 AI 调用（Chat + Embedding）替换为 Spring AI `ChatClient` + `EmbeddingModel`；RAG 基础设施（VectorCacheService + DocumentChunkService + ContentIndexService）保留不变；全局 javax→jakarta 命名空间迁移

**Tech Stack:** Spring Boot 3.4.3, Spring AI Alibaba 1.0.0-M6, MyBatis-Plus 3.5.9, JDK 17

---

### Task 1: pom.xml 依赖升级

**Files:** Modify: `portfolio-backend/pom.xml`

- [ ] **Step 1: 完整替换 pom.xml**

修改内容：spring-boot-starter-parent 2.7.18 → 3.4.3，添加 spring-ai-alibaba-bom (1.0.0-M6) + spring-ai-alibaba-starter，mybatis-plus 3.5.5 → 3.5.9，pdfbox 2.0.30 → 3.0.3，删除 spring-security-crypto 和 commons-pool2（Boot 3 自带）

完整 pom.xml 内容见设计文档 Task 1。

验证: `Select-String -Path 'portfolio-backend/pom.xml' -Pattern '3\.4\.3|spring-ai-alibaba|3\.5\.9'`

---

### Task 2: application.yml 配置迁移

**Files:** Modify: `portfolio-backend/src/main/resources/application.yml`

- [ ] **Step 1: 替换 AI 配置块**

删除 bailian: 配置块，添加 spring.ai.dashscope.* 配置（详见设计文档 Task 2）

---

### Task 3: 删除废弃文件

**Files:** Delete 3 files: AIConfig.java, RedisConfig.java, EmbeddingService.java

- [ ] **Step 1: 执行删除**

```powershell
Remove-Item 'E:\IDEA\Xinki\Xinki-Portfolio\portfolio-backend\src\main\java\com\xinki\portfolio\config\AIConfig.java' -ErrorAction SilentlyContinue
Remove-Item 'E:\IDEA\Xinki\Xinki-Portfolio\portfolio-backend\src\main\java\com\xinki\portfolio\config\RedisConfig.java' -ErrorAction SilentlyContinue
Remove-Item 'E:\IDEA\Xinki\Xinki-Portfolio\portfolio-backend\src\main\java\com\xinki\portfolio\service\EmbeddingService.java' -ErrorAction SilentlyContinue
```

---

### Task 4: javax → jakarta 全局迁移

**Files:** 所有含 javax.* 导入的 Java 文件

- [ ] **Step 1: 批量替换 javax.servlet / javax.annotation / javax.persistence → jakarta.*

```powershell
$javaFiles = Get-ChildItem -Path 'E:\IDEA\Xinki\Xinki-Portfolio\portfolio-backend\src\main\java' -Recurse -Filter '*.java'
foreach ($f in $javaFiles) {
    $c = [System.IO.File]::ReadAllText($f.FullName, [System.Text.UTF8Encoding]::new($true))
    $orig = $c
    $c = $c.Replace('javax.servlet', 'jakarta.servlet')
    $c = $c.Replace('javax.annotation', 'jakarta.annotation')
    $c = $c.Replace('javax.persistence', 'jakarta.persistence')
    if ($c -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $c, [System.Text.UTF8Encoding]::new($false))
        Write-Output "Updated: $($f.Name)"
    }
}
```

---

### Task 5: ContentIndexService — EmbeddingService → EmbeddingModel

**Files:** Modify: `portfolio-backend/src/main/java/com/xinki/portfolio/service/ContentIndexService.java`

- [ ] **Step 1: 替换导入、注入、调用**

1. import: `com.xinki.portfolio.service.EmbeddingService` → `org.springframework.ai.embedding.EmbeddingModel`
2. 字段: `EmbeddingService embeddingService` → `EmbeddingModel embeddingModel`
3. generateEmbedding → embed(content) 返回 List<Double> 转 float[]
4. embeddingService.serialize(vec) → 内联 serializeVec(vec)

```powershell
$path = 'E:\IDEA\Xinki\Xinki-Portfolio\portfolio-backend\src\main\java\com\xinki\portfolio\service\ContentIndexService.java'
$c = [System.IO.File]::ReadAllText($path, [System.Text.UTF8Encoding]::new($true))
$c = $c.Replace('import com.xinki.portfolio.service.EmbeddingService;', 'import org.springframework.ai.embedding.EmbeddingModel;')
$c = $c.Replace('private final EmbeddingService embeddingService;', 'private final EmbeddingModel embeddingModel;')
$c = $c.Replace('embeddingService.generateEmbedding(content)', 'embed(content)')
$c = $c.Replace('embeddingService.serialize(vec)', 'serializeVec(vec)')
[System.IO.File]::WriteAllText($path, $c, [System.Text.UTF8Encoding]::new($false))
```

然后手动在 indexContent 方法前添加 embed() 和 serializeVec() 两个辅助方法（详见设计文档 Task 5）。

---

### Task 6: 重写 AIChatServiceImpl（ChatClient + Flux）

**Files:** Modify: `portfolio-backend/src/main/java/com/xinki/portfolio/service/impl/AIChatServiceImpl.java`

- [ ] **Step 1: 完全重写文件**

注入 ChatClient.Builder + EmbeddingModel（替代 AIConfig + EmbeddingService）。
chat() 用 chatClient.call().content()，chatStream() 用 chatClient.stream().content() + Flux.subscribe()。
searchKnowledge() 用 embeddingModel.embed()。
删除 callOpenAI()、buildMessages() 等 HttpClient 方法。
完整代码见设计文档 Task 6。

验证:
```powershell
Select-String -Path 'portfolio-backend/src/main/java/com/xinki/portfolio/service/impl/AIChatServiceImpl.java' -Pattern 'ChatClient|EmbeddingModel|Flux'
```

---

### Task 7: 重写 AIContentServiceImpl

**Files:** Modify: `portfolio-backend/src/main/java/com/xinki/portfolio/service/impl/AIContentServiceImpl.java`

- [ ] **Step 1: ChatClient 重写 + PDFBox 3.x API**

注入 ChatClient.Builder，PDFBox 用 Loader.loadPDF()（3.x API）。完整代码见设计文档 Task 7。

---

### Task 8: 重写 AdminAIServiceImpl

**Files:** Modify: `portfolio-backend/src/main/java/com/xinki/portfolio/service/impl/AdminAIServiceImpl.java`

- [ ] **Step 1: ChatClient 重写 + PDFBox 3.x API**

注入 ChatClient.Builder，PDFBox 用 Loader.loadPDF()。完整代码见设计文档 Task 8。

---

### Task 9: 更新 Docker 部署文件

**Files:** Modify: `Dockerfile.backend`, `deploy/application-prod.yml`

- [ ] **Step 1: Dockerfile.backend** — eclipse-temurin:17-jre-alpine → eclipse-temurin:21-jre-alpine
- [ ] **Step 2: deploy/application-prod.yml** — bailian.* → spring.ai.dashscope.*

---

### Task 10: 构建验证

- [ ] **Step 1: 编译验证**

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.7.6-hotspot'
cd E:\IDEA\Xinki\Xinki-Portfolio\portfolio-backend
mvn compile -T 4 2>&1 | Select-Object -Last 30
```

预期: BUILD SUCCESS

- [ ] **Step 2: 修复编译错误（如有）**

---

### Task 11: Git 提交

- [ ] **Step 1: 提交所有变更**

```bash
git add -A
git commit -m "feat: migrate to Spring AI Alibaba framework [doc]

- Upgrade Spring Boot 2.7.18 -> 3.4.3 (javax -> jakarta)
- Replace direct DashScope HttpClient calls with Spring AI ChatClient + EmbeddingModel
- Remove AIConfig, RedisConfig, EmbeddingService (replaced by Spring AI auto-config)
- Rewrite AIChatServiceImpl with ChatClient + Flux streaming
- Rewrite AIContentServiceImpl and AdminAIServiceImpl with ChatClient
- Update ContentIndexService to use EmbeddingModel
- Upgrade MyBatis-Plus 3.5.5 -> 3.5.9, PDFBox 2.0.30 -> 3.0.3
- Update Dockerfile.backend to eclipse-temurin:21-jre-alpine
- Update deploy/application-prod.yml AI config"
```

---

### Task 12: 更新 AGENTS.md

**Files:** Modify: `AGENTS.md`

- [ ] **Step 1: 更新技术栈和目录结构描述**
