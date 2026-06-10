# Xinki Portfolio — AGENTS.md

## 项目概述

Xinki 的个人综合主页，水墨画风，包含首页/作品集/关于我/联系方式 + 全局悬浮 AI 助手（知识库 RAG）+ 管理后台。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 前端 | Vue 3 + Vite + TypeScript + Pinia + Vue Router 4 |
| AI | 阿里百炼 API (DashScope) — 兼容 OpenAI chat/completions 格式 |
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
│       │   ├── config/               # WebMvc, MyBatisPlus, AIConfig
│       │   ├── controller/           # Home, Project, About, Contact, AIChat, Admin
│       │   ├── dto/                  # LoginDTO, ChatRequestDTO, ChatResponseDTO
│       │   ├── entity/               # 7 entities (User, Project, Skill, TimelineEvent, ContactMessage, KnowledgeBase, ChatHistory)
│       │   ├── mapper/               # MyBatis-Plus BaseMapper
│       │   ├── service/              # 接口 + impl/
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
│       ├── api/                      # axios 封装 + 6 个 API 模块
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
| POST | `/api/admin/login` | 后台登录 |
| CRUD | `/api/admin/*` | 管理 CRUD（需 JWT） |

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
