# Xinki Portfolio

Xinki 的个人综合主页，水墨画风，包含首页 / 作品集 / 关于我 / 联系方式，全局悬浮 AI 助手（知识库 RAG），以及管理后台。

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
├── portfolio-backend/                # Spring Boot 后端
│   └── src/main/java/com/xinki/portfolio/
│       ├── common/                   # Result, PageResult
│       ├── config/                   # WebMvc, MyBatisPlus, AIConfig, OssConfig
│       ├── controller/               # 控制器层
│       ├── dto/                      # 数据传输对象
│       ├── entity/                   # 实体类（7张表）
│       ├── mapper/                   # MyBatis-Plus Mapper
│       ├── service/                  # 业务逻辑层
│       └── util/                     # JwtUtil
├── portfolio-frontend/               # Vue 3 前端
│   └── src/
│       ├── api/                      # axios 封装 + 上传模块
│       ├── components/               # 公共组件
│       ├── router/                   # 路由（12条，含 auth 守卫）
│       ├── stores/                   # Pinia 状态管理
│       ├── styles/                   # 水墨主题 CSS 变量 & 动画
│       └── views/                    # 页面视图 + 管理后台 CRUD
└── docs/                             # 设计文档
```

## 快速启动

### 1. 数据库

创建 MySQL 数据库 `xinki_portfolio`（字符集 `utf8mb4`），执行 `portfolio-backend/src/main/resources/db/schema.sql`。

### 2. 后端

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

## API 概览

统一响应格式：`{ code: Integer, message: String, data: Object }`

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
| `project` | 作品 |
| `skill` | 技能 |
| `timeline_event` | 时间线事件 |
| `contact_message` | 留言 |
| `knowledge_base` | AI 知识库 |
| `chat_history` | AI 对话历史 |

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
