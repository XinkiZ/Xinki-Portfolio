# Xinki Portfolio — AGENTS.md

## 项目概述

Xinki 的个人综合主页，水墨画风，包含首页/作品集/关于我/联系方式 + 全局悬浮 AI 助手（知识库 RAG）+ 管理后台。支持 AI 自主生成作品简介和 HTML 描述（上传 PDF/README 文件）。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 前端 | Vue 3 + Vite + TypeScript + Pinia + Vue Router 4 |
| AI | 阿里百炼 API (DashScope) — chat/completions + text-embedding-v3 |
| 存储 | 阿里云 OSS (aliyun-sdk-oss 3.17.4) + Redis (向量缓存) |
| 鉴权 | JWT (java-jwt 4.4.0) + BCrypt |

## 项目结构

`
Xinki-Portfolio/
├── portfolio-backend/                # Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/xinki/portfolio/
│       │   ├── PortfolioApplication.java
│       │   ├── common/               # Result, PageResult
│       │   ├── config/               # WebMvc, MyBatisPlus, AIConfig, OssConfig, RagConfig
│       │   ├── controller/           # Home, Project, About, Contact, AIChat（含 generate-content + analyze-document）, Admin, OssController
│       │   ├── dto/                  # LoginDTO, ChatRequestDTO, ChatResponseDTO, GenerateContentResponse, DocumentAnalysisDTO
│       │   ├── entity/               # 7 entities (User, Project, Skill, TimelineEvent, ContactMessage, KnowledgeBase, ChatHistory)
│       │   ├── mapper/               # MyBatis-Plus BaseMapper
│       │   ├── service/              # 接口 + impl/（含 OssService, AIContentService, AdminAIService）
│       │   └── util/                 # JwtUtil
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql
├── portfolio-frontend/               # Vue 3
│   ├── vite.config.ts                # @ 别名 + /api proxy → :8080
│   └── src/
│       ├── main.ts                   # 入口：Pinia + Router + 全局样式
│       ├── App.vue                   # Header + router-view + Footer + AIChatBubble
│       ├── router/index.ts           # 13 条路由（含管理后台 auth 守卫）
│       ├── stores/chat.ts            # AI 聊天 Pinia store
│       ├── api/                      # axios 封装 + upload 模块 + ai 模块
│       ├── views/                    # 公开页面 + admin/ 管理 CRUD（含 个人设置 + AI 文档分析助手）
│       ├── components/               # AppHeader, AppFooter, AIChatBubble, ProjectCard
│       └── styles/                   # variables.css, global.css, ink-effects.css
└── docs/superpowers/                 # 设计文档
    ├── specs/2026-06-09-portfolio-design.md
    └── plans/2026-06-09-portfolio-plan.md
`

## 数据库

7 张表：user、project（含 summary 简介字段）、contact_message、skill、	imeline_event、knowledge_base、chat_history

建表 SQL：portfolio-backend/src/main/resources/db/schema.sql

数据库名：xinki_portfolio，字符集 utf8mb4

> 已部署数据库需执行迁移：
> `ALTER TABLE project ADD COLUMN summary VARCHAR(500) COMMENT '简介，卡片展示用' AFTER title;`

## 启动方式

### 环境变量

启动后端前需确保 Redis 运行在 localhost:6379（无密码），并设置以下环境变量：

``powershell
# OSS 凭据（必需）
`env:OSS_ACCESS_KEY_ID = "your-access-key-id"
`env:OSS_ACCESS_KEY_SECRET = "your-access-key-secret"

# OSS Bucket 名称（必需）
`env:OSS_BUCKET_NAME = "your-bucket-name"

# OSS Endpoint（可选，默认 oss-cn-hangzhou.aliyuncs.com）
`env:OSS_ENDPOINT = "oss-cn-beijing.aliyuncs.com"
``

### 后端
`ash
cd portfolio-backend
# 先创建 MySQL 数据库并执行 db/schema.sql
# 修改 application.yml 中的数据库密码和百炼 API Key
mvn spring-boot:run
# 运行在 http://localhost:8080
`

### 前端
`ash
cd portfolio-frontend
npm install    # 首次
npm run dev    # http://localhost:5173，/api 自动 proxy 到 8080
`

## API 概览

统一响应：{ code: Integer, message: String, data: Object }

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/home | 首页 |
| GET | /api/projects | 作品列表（返回 summary 用于卡片展示） |
| GET | /api/projects/{id} | 作品详情（返回 summary + HTML description） |
| GET | /api/about | 关于我 |
| POST | /api/contact | 留言 |
| POST | /api/ai/chat | AI 聊天（非流式，兼容） |
| POST | /api/ai/chat/stream | AI 聊天（流式 SSE，逐字输出） |
| GET | /api/ai/chat/{sessionId} | 对话历史 |
| DELETE | /api/ai/chat/{sessionId} | 清除历史 |
| POST | /api/ai/chat/generate-content | AI 生成作品内容（上传文件 → summary + HTML description） |
| POST | /api/admin/knowledge/import | 导入知识库文件（PDF/MD/TXT → 分块 → 向量化 → 入库 → 去重） |
| DELETE | /api/admin/knowledge/file/{hash} | 按 source_hash 批量删除文件所有片段 |
| POST | /api/admin/projects/reindex | 重建全部内容 RAG 索引（作品/技能/经历） |
| POST | /api/admin/ai/analyze-document | AI 文档分析（上传 PDF/README 提取项目+技能） |
| GET | /api/admin/profile | 获取当前用户信息（需 JWT） |
| PUT | /api/admin/profile | 更新用户名、头像、简介（需 JWT） |
| PUT | /api/admin/profile/password | 修改密码（需旧密码验证，需 JWT） |
| POST | /api/upload | 图片上传（→ 阿里云 OSS） |
| POST | /api/admin/login | 后台登录 |
| CRUD | /api/admin/* | 管理 CRUD（需 JWT） |

### OSS 上传

POST /api/upload 接收 multipart/form-data，字段名 ile。

返回：{ code: 200, data: { url: "https://bucket.endpoint/portfolio/yyyy/MM/dd/uuid.ext" } }

存储路径规则：portfolio/{yyyy/MM/dd}/{uuid}.{ext}，自动按日期分目录。

管理后台中作品封面和技能图标均支持：点击上传文件（自动调用上传接口并回填 URL），或直接粘贴图片 URL。

### AI 生成作品内容

POST /api/ai/chat/generate-content 接收 multipart/form-data，字段名 ile。

支持格式：.pdf、.md、.txt、.markdown

返回：{ summary: "简介", description: "HTML 描述" } — AI 自主判定输出纯文字或富 HTML。

管理后台作品编辑弹窗（ProjectManage.vue）和 AI 文档分析助手（AdminAiAssistant.vue）均集成此功能。

## 样式约定

- 水墨主题 CSS 变量定义在 src/styles/variables.css（--ink-* 前缀）
- 全局样式在 src/styles/global.css
- 动画效果在 src/styles/ink-effects.css
- 字体：衬线 "Noto Serif SC", "SimSun"，无衬线 "Noto Sans SC", "PingFang SC"
- 印章红：#c43a31


## RAG 知识库

### 检索流程

```
用户提问 → 生成 query embedding (text-embedding-v3, 1024维)
→ 从 Redis 缓存获取全量向量 → 余弦相似度 Top-K
→ 拼接相关文本片段作为 context 注入 system prompt
```

### 导入流程

```
上传 PDF/MD/TXT → 文本提取 → 按段落/标题分块（≤800字/块，≤50块/文件）
→ 逐块调用 text-embedding-v3 → 存入 knowledge_base + Redis
→ SHA-256 去重（重复上传会覆盖旧数据）
```

### 配置项 (application.yml rag.*)

| 参数 | 默认值 | 说明 |
|------|--------|------|
| top-k | 3 | 检索返回的最相关片段数 |
| max-chunks-per-file | 50 | 单文件最大分块数 |
| min-chunk-length | 50 | 最小片段字数 |
| max-chunk-length | 800 | 最大片段字数 |
| max-context-chars | 2000 | 注入 prompt 的最大总字符数 |

### 内容自动索引（ContentIndexService）

管理后台 CRUD 作品/技能/经历时，自动同步 knowledge_base：
- source_hash = "project:{id}" / "skill:{id}" / "timeline:{id}"
- 已发布作品才索引，下架自动删除
- POST /api/admin/projects/reindex 一键批量重建全部

每次 AI 对话同时注入 RAG 语义结果 + 作品/技能全量目录（buildCatalog()）。

### Redis 降级

Redis 不可用时自动回退到内存缓存（启动时从 DB 全量加载 embedding），不影响核心功能。

## 重要注意事项

1. **所有源文件必须使用 UTF-8 无 BOM 编码** — BOM 会导致前端 JSON 解析失败
2. **MySQL JDBC URL 需要 llowPublicKeyRetrieval=true** — MySQL 8.0+ 要求
3. **AI 端点**：https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
4. **默认管理员**：schema.sql 中有预设用户（需改 BCrypt 密码）
5. **前端 dev 已配置 Vite proxy**：/api → http://localhost:8080
6. 不要提交 
ode_modules/、	arget/、.idea/、.m2/
7. **OSS Endpoint 必须与 Bucket 所在区域一致**，否则报 AccessDenied
8. OSS Client 为 Spring 单例 Bean，应用关闭时自动 shutdown
9. **已部署数据库需执行迁移**：ALTER TABLE project ADD COLUMN summary VARCHAR(500) COMMENT '简介，卡片展示用' AFTER title;
10. **AI 生成质量参数**：max_tokens=4096（足够支撑完整 HTML）、	emperature=0.3（保证输出格式稳定）
11. **Java 字符串中双引号转义**：\" 正确，\\" 会导致编译错误 需要';'，详见下方易错点章节
12. **Redis 必须运行**：向量缓存依赖 Redis localhost:6379，不可用时自动降级内存缓存
13. **config/RedisConfig.java 需手动删除**：Spring Boot 自动配置 StringRedisTemplate，该文件导致 bean 冲突
14. **知识库表结构已变更**：旧 question+answer → 新 content+source_file+source_hash+chunk_index，旧表需 DROP 重建
15. **TimelineEvent.java 缺少 summary 字段**：DB schema 有 summary 列但实体类未映射，ContentIndexService 不使用该字段
16. **ContentIndexService 联动**：作品/技能/经历 CRUD 时自动调用 indexXxx() 或 removeXxxIndex()

## 文件操作规范（写给 AI — 必须遵守）

> 注意：此节是多次踩坑后的血的教训，后续 AI 助手必须逐条遵守。

### 编码铁律

1. 所有源文件必须是 UTF-8 without BOM。BOM 会导致前端 Vite/esbuild 解析失败。
2. 包含中文的文件尤其容易在写入时被损坏，必须用正确 API。

### 写入文件：正确 vs 错误

| 操作 | 禁止做法 | 正确做法 |
|------|----------|----------|
| 创建新文件 | Set-Content -Encoding UTF8 | [System.IO.File]::WriteAllText(path, content, UTF8Encoding(false)) |
| 读取文件 | Get-Content -Raw | [System.IO.File]::ReadAllText(path, UTF8Encoding(true)) |
| 修改已有文件 | Get-Content + Select-String 截取拼装后 Set-Content | 先 ReadAllText -> 在内存中修改 -> WriteAllText |

为什么 Set-Content -Encoding UTF8 不可靠：PowerShell 版本差异会导致实际输出为 UTF-8 with BOM 或使用系统代码页，造成中文乱码。

### 标准代码模板

创建新文件：
`powershell
`path = 'E:\IDEA\Xinki\Xinki-Portfolio\目标文件路径'
`content = @'
文件完整内容（中文直接写）
'@
[System.IO.File]::WriteAllText(`path, `content, [System.Text.UTF8Encoding]::new(`alse))
`

修改已有文件：
`powershell
`path = 'E:\IDEA\Xinki\Xinki-Portfolio\目标文件路径'
`content = [System.IO.File]::ReadAllText(`path, [System.Text.UTF8Encoding]::new(`	rue))
`content = `content.Replace('旧文本', '新文本')
[System.IO.File]::WriteAllText(`path, `content, [System.Text.UTF8Encoding]::new(`alse))
`

### 写入后验证（必须执行）

每次文件写入后检查：
1. 无 BOM：ReadAllBytes 检查前 3 字节不是 EF BB BF
2. 中文未损坏：Get-Content -Encoding UTF8 | Select-String 已知中文关键词


### 前端 MyBatis-Plus Page 响应数据提取

后端分页接口返回 `Result<Page<T>>`，Page 对象结构为 `{ records: [...], total: N, current: N }`。

前端必须取 `r.data.records` 而非 `r.data`：

| 正确 | 错误 |
|------|------|
| `list.value = r.data.records \|\| []` | `list.value = r.data \|\| []` |

错误写法会导致 `v-for` 迭代 Page 对象属性（records/total/current 等），表格渲染异常，表现为"无法添加/删除"。


### Java 字符串转义易错点（血的教训）

修改 Java 文件中的字符串（如 AI prompt）时，双引号转义极易出错：

| 写法 | 结果 | 是否正确 |
|------|------|----------|
| "...\\"中文\\"..." | \\ 转义为 \，紧随的 " 终止字符串 → 编译错误 需要';' | ❌ |
| "...\"中文\"..." | \" 转义为字面量双引号，字符串内容为 "中文" | ✅ |

规则：Java 源码中要嵌入字面量 "，用 \"（单个反斜杠），不要用 \\"（两个反斜杠）。

### 其他禁止项

- 禁止使用 apply_patch 工具 — 该工具在此环境中不可靠，统一用 Shell 写入
- 修改路由文件 — 必须读取全文后在内存中修改再写回，不要用 Select-String 截取拼装
- 修改 pom.xml — 同上，全文读取 -> 内存修改 -> 写回
- Python 文件 — 同样遵循 UTF-8 无 BOM 规则

### 前端文件特别说明

- .vue 文件：template、script、style 中的中文必须可读
- .ts 文件：import 路径、路由 meta.title 等中文必须正确
- router/index.ts：新增路由时，确保花括号不重复，缩进与现有风格一致（2 空格）

## Git 提交规范

每次 git commit 前，AI 助手必须执行以下操作：

1. **检查变更范围** — 确认本次修改涉及的文件和模块
2. **同步更新 README.md 和 AGENTS.md** — 如果变更影响了以下任何内容，必须同步更新：
   - 项目功能概览（新增/移除功能）
   - 目录结构（新增/删除模块或文件）
   - API 接口（路由变更、入参出参变更）
   - 启动方式（环境变量、依赖、配置变更）
   - 易错点 / 踩坑记录（遇到新的环境或配置陷阱）
3. **Commit message 中注明** — 若本次 README.md / AGENTS.md 有更新，在 commit message 中标注 [doc]

此规范确保后续 AI 能通过 README.md + AGENTS.md 准确识别项目信息。