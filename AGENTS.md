# Xinki Portfolio — AGENTS.md

## 项目概述

Xinki 的个人综合主页，水墨画风，包含首页/作品集/关于我/联系方式 + 全局悬浮 AI 助手（知识库 RAG）+ 管理后台。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 前端 | Vue 3 + Vite + TypeScript + Pinia + Vue Router 4 |
| AI | 阿里百炼 API (DashScope) — 兼容 OpenAI chat/completions 格式 |
| 存储 | 阿里云 OSS (aliyun-sdk-oss 3.17.4) |
| 鉴权 | JWT (java-jwt 4.4.0) + BCrypt |

## 项目结构

```
Xinki-Portfolio/
├── portfolio-backend/                # Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/xinki/portfolio/
│       │   ├── PortfolioApplication.java
│       │   ├── common/               # Result, PageResult
│       │   ├── config/               # WebMvc, MyBatisPlus, AIConfig, OssConfig
│       │   ├── controller/           # Home, Project, About, Contact, AIChat, Admin, OssController
│       │   ├── dto/                  # LoginDTO, ChatRequestDTO, ChatResponseDTO
│       │   ├── entity/               # 7 entities (User, Project, Skill, TimelineEvent, ContactMessage, KnowledgeBase, ChatHistory)
│       │   ├── mapper/               # MyBatis-Plus BaseMapper
│       │   ├── service/              # 接口 + impl/（含 OssService）
│       │   └── util/                 # JwtUtil
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql
├── portfolio-frontend/               # Vue 3
│   ├── vite.config.ts                # @ 别名 + /api proxy → :8080
│   └── src/
│       ├── main.ts                   # 入口：Pinia + Router + 全局样式
│       ├── App.vue                   # Header + router-view + Footer + AIChatBubble
│       ├── router/index.ts           # 12 条路由（含管理后台 auth 守卫）
│       ├── stores/chat.ts            # AI 聊天 Pinia store
│       ├── api/                      # axios 封装 + upload 模块
│       ├── views/                    # 公开页面 + admin/ 管理 CRUD
│       ├── components/               # AppHeader, AppFooter, AIChatBubble, ProjectCard
│       └── styles/                   # variables.css, global.css, ink-effects.css
└── docs/superpowers/                 # 设计文档
    ├── specs/2026-06-09-portfolio-design.md
    └── plans/2026-06-09-portfolio-plan.md
```

## 数据库

7 张表：`user`, `project`, `contact_message`, `skill`, `timeline_event`, `knowledge_base`, `chat_history`

建表 SQL：`portfolio-backend/src/main/resources/db/schema.sql`

数据库名：`xinki_portfolio`，字符集 `utf8mb4`

## 启动方式

### 环境变量

启动后端前需设置以下环境变量：

```powershell
# OSS 凭据（必需）
$env:OSS_ACCESS_KEY_ID = "your-access-key-id"
$env:OSS_ACCESS_KEY_SECRET = "your-access-key-secret"

# OSS Bucket 名称（必需）
$env:OSS_BUCKET_NAME = "your-bucket-name"

# OSS Endpoint（可选，默认 oss-cn-hangzhou.aliyuncs.com）
$env:OSS_ENDPOINT = "oss-cn-beijing.aliyuncs.com"
```

### 后端
```bash
cd portfolio-backend
# 先创建 MySQL 数据库并执行 db/schema.sql
# 修改 application.yml 中的数据库密码和百炼 API Key
mvn spring-boot:run
# 运行在 http://localhost:8080
```

### 前端
```bash
cd portfolio-frontend
npm install    # 首次
npm run dev    # http://localhost:5173，/api 自动 proxy 到 8080
```

## API 概览

统一响应：`{ code: Integer, message: String, data: Object }`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/home` | 首页 |
| GET | `/api/projects` | 作品列表 |
| GET | `/api/projects/{id}` | 作品详情 |
| GET | `/api/about` | 关于我 |
| POST | `/api/contact` | 留言 |
| POST | `/api/ai/chat` | AI 聊天 |
| GET | `/api/ai/chat/{sessionId}` | 对话历史 |
| DELETE | `/api/ai/chat/{sessionId}` | 清除历史 |
| POST | `/api/upload` | 图片上传（→ 阿里云 OSS） |
| POST | `/api/admin/login` | 后台登录 |
| CRUD | `/api/admin/*` | 管理 CRUD（需 JWT） |

### OSS 上传

`POST /api/upload` 接收 `multipart/form-data`，字段名 `file`。

返回：`{ code: 200, data: { url: "https://bucket.endpoint/portfolio/yyyy/MM/dd/uuid.ext" } }`

存储路径规则：`portfolio/{yyyy/MM/dd}/{uuid}.{ext}`，自动按日期分目录。

管理后台中作品封面和技能图标均支持：点击上传文件（自动调用上传接口并回填 URL），或直接粘贴图片 URL。

## 样式约定

- 水墨主题 CSS 变量定义在 `src/styles/variables.css`（`--ink-*` 前缀）
- 全局样式在 `src/styles/global.css`
- 动画效果在 `src/styles/ink-effects.css`
- 字体：衬线 `"Noto Serif SC", "SimSun"`，无衬线 `"Noto Sans SC", "PingFang SC"`
- 印章红：`#c43a31`

## 重要注意事项

1. **所有源文件必须使用 UTF-8 无 BOM 编码** — BOM 会导致前端 JSON 解析失败
2. **MySQL JDBC URL 需要 `allowPublicKeyRetrieval=true`** — MySQL 8.0+ 要求
3. **AI 端点**：`https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
4. **默认管理员**：schema.sql 中有预设用户（需改 BCrypt 密码）
5. **前端 dev 已配置 Vite proxy**：`/api` → `http://localhost:8080`
6. 不要提交 `node_modules/`、`target/`、`.idea/`
7. **OSS Endpoint 必须与 Bucket 所在区域一致**，否则报 AccessDenied
8. OSS Client 为 Spring 单例 Bean，应用关闭时自动 shutdown
## Git 提交规范

每次 `git commit` 前，AI 助手必须执行以下操作：

1. **检查变更范围** — 确认本次修改涉及的文件和模块
2. **同步更新 `README.md`** — 如果变更影响了以下任何内容，必须同步更新 README.md 中对应章节：
   - 项目功能概览（新增/移除功能）
   - 目录结构（新增/删除模块或文件）
   - API 接口（路由变更、入参出参变更）
   - 启动方式（环境变量、依赖、配置变更）
   - 易错点 / 踩坑记录（遇到新的环境或配置陷阱）
3. **Commit message 中注明** — 若本次 README.md 有更新，在 commit message 中标注 `[doc]`

此规范确保后续 AI 能通过 README.md + AGENTS.md 准确识别项目信息。
