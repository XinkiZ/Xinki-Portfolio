# Xinki Portfolio — 个人综合主页设计文档

> 创建日期：2026-06-09  
> 版本：v1.0  
> 状态：已确认

---

## 一、项目概述

一个水墨画风的中文个人综合主页，包含首页展示、作品集、关于我、联系方式四大页面，以及全局悬浮 AI 助手（带个人知识库问答）。技术栈为 Spring Boot + Vue 3 + Vite，前后端分离部署（Nginx + Spring Boot）。

---

## 二、技术架构

```
                   用户浏览器
                        |
                   Nginx (:80)
                  /            \
         Vue 静态资源       /api/* → Spring Boot
                                    |
                               MySQL 数据库
```

| 层级 | 技术选型 |
|------|----------|
| 前端 | Vue 3 + Vite + TypeScript + Pinia |
| 后端 | Spring Boot 2.7+ + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| AI | OpenAI API + 本地知识库（向量检索） |
| 鉴权 | JWT Token |
| 部署 | Nginx 反向代理，前后端独立 |

---

## 三、页面结构与路由

| 路由 | 页面 | 说明 |
|------|------|------|
| `/` | 首页 | Hero 水墨动画 + 个人简介 + 导航 |
| `/projects` | 作品集 | 卡片网格 + 标签筛选 + 详情 |
| `/about` | 关于我 | 个人介绍 + 技能树 + 时间线 |
| `/contact` | 联系方式 | 留言表单 + 社交链接 |
| 全局悬浮 | AI 助手 | 右下角砚台图标 → 弹出水墨聊天窗 |

AI 助手为全局组件（AIChatBubble.vue），不占用路由，悬浮于所有页面之上。

### 水墨元素映射

| 页面 | 水墨意象 |
|------|----------|
| 首页 | 山水 |
| 作品集 | 墨竹 |
| 关于我 | 笔触 |
| 联系方式 | 印章 |
| AI 助手 | 砚台 |

---

## 四、数据库设计

### user（管理员）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 加密密码 |
| avatar | VARCHAR(500) | 头像 URL |
| intro | TEXT | 个人简介 |

### project（作品集）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| title | VARCHAR(200) | 项目名称 |
| description | TEXT | 项目描述 |
| cover_url | VARCHAR(500) | 封面图 |
| demo_url | VARCHAR(500) | 演示链接 |
| github_url | VARCHAR(500) | 源码链接 |
| tags | VARCHAR(500) | 标签（逗号分隔） |
| sort_order | INT | 排序 |
| created_at | DATETIME | 创建时间 |
| is_published | TINYINT | 是否发布 |

### contact_message（留言）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(100) | 姓名 |
| email | VARCHAR(200) | 邮箱 |
| content | TEXT | 留言内容 |
| is_read | TINYINT | 是否已读 |
| created_at | DATETIME | 创建时间 |

### skill（技能）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(100) | 技能名称 |
| category | VARCHAR(50) | 分类 |
| level | INT | 熟练度(1-100) |
| icon | VARCHAR(500) | 图标 |
| sort_order | INT | 排序 |

### timeline_event（经历时间线）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| title | VARCHAR(200) | 标题 |
| description | TEXT | 描述 |
| type | VARCHAR(20) | 类型（教育/工作/项目） |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期（空表至今） |
| sort_order | INT | 排序 |

### knowledge_base（AI 知识库）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| question | TEXT | 示例问题 |
| answer | TEXT | 回答内容 |
| embedding | JSON/VECTOR | 向量嵌入 |
| category | VARCHAR(50) | 分类 |

### chat_history（AI 对话记录）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| session_id | VARCHAR(64) | 会话 ID |
| role | VARCHAR(20) | 角色(user/assistant) |
| content | TEXT | 消息内容 |
| created_at | DATETIME | 创建时间 |

---

## 五、API 设计

统一响应格式：`{ code: Integer, message: String, data: Object }`

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/home` | 首页信息 |
| GET | `/api/projects` | 作品列表（分页+标签） |
| GET | `/api/projects/{id}` | 作品详情 |
| GET | `/api/skills` | 技能列表 |
| GET | `/api/timeline` | 经历时间线 |
| GET | `/api/about` | 关于我详情 |
| POST | `/api/contact` | 提交留言 |
| POST | `/api/ai/chat` | AI 聊天 |
| GET | `/api/ai/chat/{sessionId}` | 对话历史 |
| DELETE | `/api/ai/chat/{sessionId}` | 清除历史 |

### 管理接口（需 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/login` | 管理员登录 |
| GET | `/api/admin/dashboard` | 管理概览 |
| CRUD | `/api/admin/projects` | 作品管理 |
| CRUD | `/api/admin/skills` | 技能管理 |
| CRUD | `/api/admin/timeline` | 时间线管理 |
| CRUD | `/api/admin/knowledge` | AI 知识库管理 |
| GET | `/api/admin/messages` | 留言管理 |

---

## 六、视觉风格

- **主题**：水墨画风（ink wash painting）
- **色调**：黑白为主，淡雅灰调，点缀朱红印章色
- **字体**：思源宋体 / 站酷文艺体（标题），思源黑体（正文）
- **动画**：水墨晕染、墨滴扩散、笔触飞白效果
- **响应式**：桌面端 + 平板 + 手机完整适配

---

## 七、项目结构

```
Xinki-Portfolio/
├── portfolio-frontend/          Vue 3 前端项目
│   ├── src/
│   │   ├── views/              页面组件
│   │   ├── components/         通用组件（含 AIChatBubble）
│   │   ├── stores/             Pinia 状态管理
│   │   ├── api/                API 请求封装
│   │   ├── router/             路由配置
│   │   ├── assets/             静态资源（水墨素材）
│   │   └── styles/             全局样式
│   └── ...
├── portfolio-backend/           Spring Boot 后端项目
│   ├── src/main/java/
│   │   ├── controller/         REST 控制器
│   │   ├── service/            业务服务
│   │   ├── mapper/             MyBatis-Plus Mapper
│   │   ├── entity/             实体类
│   │   ├── dto/                数据传输对象
│   │   ├── config/             配置类
│   │   └── util/               工具类
│   └── src/main/resources/
│       └── application.yml     配置文件
└── docs/superpowers/specs/     设计文档
```

---

## 八、非功能需求

- 前端首屏加载 < 2s
- AI 聊天响应流式返回（SSE）
- 管理后台需登录鉴权
- 支持移动端响应式
- 数据库需支持向量存储（MySQL 8.0+ 或备选 PostgreSQL）
