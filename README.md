# Xinki Portfolio

Xinki 的个人综合主页，水墨画风，包含首页 / 作品集 / 关于我 / 联系方式，全局悬浮 AI 助手（知识库 RAG + 向量语义检索 + 流式 SSE），以及管理后台。支持上传 PDF/MD/TXT 知识库文件自动分块向量化，AI 自主生成作品简介和 HTML 描述，作品/技能/经历变动自动同步 RAG 索引。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 前端 | Vue 3 + Vite + TypeScript + Pinia + Vue Router 4 |
| AI | 阿里百炼 API (DashScope) — chat/completions + text-embedding-v3 |
| 存储 | 阿里云 OSS (aliyun-sdk-oss 3.17.4) + Redis (向量缓存) |
| 鉴权 | JWT (java-jwt 4.4.0) + BCrypt |
| PDF | Apache PDFBox 2.0.30 |

## 项目结构

```
Xinki-Portfolio/
├── portfolio-backend/                # Spring Boot 后端
│   └── src/main/java/com/xinki/portfolio/
│       ├── common/                   # Result, PageResult
│       ├── config/                   # WebMvc, MyBatisPlus, AIConfig, OssConfig, RagConfig
│       ├── controller/               # 控制器层（含 AdminAIController 文档分析）
│       ├── dto/                      # 数据传输对象
│       ├── entity/                   # 实体类（7张表）
│       ├── mapper/                   # MyBatis-Plus Mapper
│       ├── service/                  # 业务逻辑层（含 EmbeddingService, VectorCacheService, DocumentChunkService, ContentIndexService）
│       └── util/                     # JwtUtil
├── portfolio-frontend/               # Vue 3 前端
│   └── src/
│       ├── api/                      # axios 封装 + 上传模块
│       ├── components/               # 公共组件
│       ├── router/                   # 路由（13条，含 auth 守卫）
│       ├── stores/                   # Pinia 状态管理
│       ├── styles/                   # 水墨主题 CSS 变量 & 动画
│       └── views/                    # 页面视图 + 管理后台 CRUD
└── docs/                             # 设计文档
```

## 快速启动

### 1. 数据库

创建 MySQL 数据库 `xinki_portfolio`（字符集 `utf8mb4`），执行 `portfolio-backend/src/main/resources/db/schema.sql`。

### 2. 后端

**前置要求：Redis 运行在 `localhost:6379`（无密码）**

```powershell
# 设置环境变量
$env:OSS_ACCESS_KEY_ID = "your-access-key-id"
$env:OSS_ACCESS_KEY_SECRET = "your-access-key-secret"
$env:OSS_BUCKET_NAME = "your-bucket-name"
$env:OSS_ENDPOINT = "oss-cn-beijing.aliyuncs.com"  # 可选，默认杭州

cd portfolio-backend
# 修改 application.yml 中的数据库密码和百炼 API Key
mvn spring-boot:run
# → http://localhost:8080
```

### 3. 前端

```bash
cd portfolio-frontend
npm install
npm run dev
# → http://localhost:5173，/api 自动 proxy 到 :8080
```


## Docker 部署（阿里云 ECS）

### 一键部署

```bash
# 1. 上传项目到 ECS
scp -r Xinki-Portfolio/ root@<ECS公网IP>:/opt/

# 2. 配置环境变量
cd /opt/Xinki-Portfolio
cp .env.example .env
vim .env  # 填入真实的 API Key、OSS 凭据

# 3. 一键部署
chmod +x deploy.sh
./deploy.sh
```

部署完成后访问 `http://<ECS公网IP>`。默认管理员：`admin` / `admin123`。

### 架构

```
Browser (80/443) → Nginx → /api/* → Backend (8080) → MySQL (3306)
                              ↓                      → Redis (6379)
                         / → Static Files (Vue SPA)
```

### 运维命令

```bash
docker compose ps                  # 服务状态
docker compose logs -f backend     # 后端日志
docker compose up -d --build       # 更新代码后重建
docker compose down                # 停止服务
```

### 前置条件

1. ECS 安全组放行 80、443 端口
2. 域名 DNS 解析到 ECS 公网 IP（可选，用于 SSL）
3. 阿里云 SSL 证书下载后放置到 `deploy/nginx/ssl/`

详细部署文档见 `AGENTS.md`。
## API 概览

统一响应格式：`{ code: Integer, message: String, data: Object }`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/home` | 首页 |
| GET | `/api/projects` | 作品列表 |
| GET | `/api/projects/{id}` | 作品详情 |
| GET | `/api/about` | 关于我 |
| POST | `/api/contact` | 留言 |
| POST | `/api/ai/chat` | AI 聊天（非流式，兼容） |
| POST | `/api/ai/chat/stream` | AI 聊天（流式 SSE，逐字输出） |
| GET | `/api/ai/chat/{sessionId}` | 对话历史 |
| DELETE | `/api/ai/chat/{sessionId}` | 清除历史 |
| POST | `/api/ai/chat/generate-content` | AI 生成作品内容（上传文件 → 自动生成简介+HTML 描述） |
| POST | `/api/admin/knowledge/import` | 导入知识库文件（PDF/MD/TXT 自动分块+向量化） |
| POST | `/api/admin/projects/reindex` | 重建全部内容 RAG 索引（作品/技能/经历） |
| POST | /api/admin/ai/analyze-document | AI 文档分析（上传 PDF/README 提取项目信息+技能） |
| GET | `/api/admin/profile` | 获取当前登录用户信息（需 JWT） |
| PUT | `/api/admin/profile` | 更新用户名、头像、个人简介（需 JWT） |
| PUT | `/api/admin/profile/password` | 修改密码（需旧密码验证，需 JWT） |
| POST | `/api/upload` | 图片上传 → 阿里云 OSS |
| POST | `/api/admin/login` | 后台登录 |
| CRUD | `/api/admin/*` | 管理 CRUD（需 JWT） |

### OSS 上传

`POST /api/upload`，字段名 `file`，`multipart/form-data`。

返回：`{ code: 200, data: { url: "https://bucket.endpoint/portfolio/yyyy/MM/dd/uuid.ext" } }`

## 数据库表

| 表名 | 说明 |
|------|------|
| `user` | 管理员用户 |
| `project` | 作品（含 `summary` 简介 + `description` HTML 详细描述） |
| `skill` | 技能 |
| `timeline_event` | 时间线事件 |
| `contact_message` | 留言 |
| `knowledge_base` | AI 知识库（content + embedding + source_file + source_hash + chunk_index） |
| `chat_history` | AI 对话历史 |


## RAG 知识库

### 检索流程
```
用户提问 → text-embedding-v3 (1024维) → Redis 取全量向量 → 余弦相似度 Top-K → 拼接上下文 + 作品/技能全量目录 → 注入 system prompt
```

### 内容自动索引
管理后台 CRUD 作品/技能/经历时，自动同步到 `knowledge_base` 表。`POST /api/admin/projects/reindex` 可批量重建全部索引。

### 配置项（application.yml `rag.*`）
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `top-k` | 3 | 语义检索返回 Top N |
| `max-context-chars` | 2000 | 注入 prompt 的上下文上限 |
| `max-chunks-per-file` | 50 | 单文件导入时最大分块数 |

## 样式约定

- 水墨主题 CSS 变量：`--ink-*` 前缀（见 `src/styles/variables.css`）
- 字体：衬线 `"Noto Serif SC", "SimSun"`，无衬线 `"Noto Sans SC", "PingFang SC"`
- 印章红：`#c43a31`

## 易错点

1. **UTF-8 无 BOM** — 所有源文件必须使用 UTF-8 无 BOM 编码，BOM 会导致前端 JSON 解析失败
2. **MySQL JDBC URL 需 `allowPublicKeyRetrieval=true`** — MySQL 8.0+ 要求
3. **OSS Endpoint 与 Bucket 区域一致** — 不一致会报 AccessDenied
4. **AI 端点** — `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`（不是标准 OpenAI 地址）
5. **Vite proxy** — 前端 dev 已配置 `/api` → `http://localhost:8080`，无需额外配置
6. **环境变量** — 启动后端前必须设置 OSS 三个环境变量，否则启动失败
7. **不要提交** — `node_modules/`、`target/`、`.idea/`
8. **默认管理员** — schema.sql 中有预设用户，需自行修改 BCrypt 密码
9. **Redis 必须运行** — 向量缓存依赖 Redis `localhost:6379`，不可用时自动降级内存缓存
10. **删除冗余配置** — `config/RedisConfig.java` 与 Spring Boot 自动配置冲突，需手动删除
11. **知识库表结构已变更** — 旧 `knowledge_base` 表需 `DROP TABLE` 后重新执行 schema.sql
