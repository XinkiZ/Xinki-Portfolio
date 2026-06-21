# Xinki Portfolio — 实现计划

> **For agentic workers:** Use superpowers:subagent-driven-development or executing-plans. Steps use checkbox syntax.

**Goal:** 构建水墨画风个人综合主页（首页/作品集/关于我/联系方式 + AI悬浮助手 + 管理后台）

**Architecture:** Spring Boot (8080) + Vue 3 (5173) + Nginx 反向代理 /api/*

**Tech Stack:** Spring Boot 2.7+, MyBatis-Plus, MySQL 8.0, Vue 3 + Vite + TypeScript + Pinia, OpenAI API

---

## 文件结构

```
E:\IDEA\Xinki\Xinki-Portfolio\
├── portfolio-backend/           Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/xinki/portfolio/
│       │   ├── PortfolioApplication.java
│       │   ├── common/          Result.java, PageResult.java
│       │   ├── config/          WebMvcConfig, MyBatisPlusConfig, AIConfig, SecurityConfig
│       │   ├── entity/          7 entities
│       │   ├── dto/             LoginDTO, ChatRequestDTO, ChatResponseDTO
│       │   ├── mapper/          7 mappers
│       │   ├── service/         5 services + impl/
│       │   ├── controller/      6 controllers
│       │   └── util/            JwtUtil.java
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql
│
├── portfolio-frontend/          Vue 3
│   ├── package.json, vite.config.ts, tsconfig.json
│   └── src/
│       ├── main.ts, App.vue
│       ├── router/index.ts
│       ├── stores/              chat.ts, app.ts
│       ├── api/                 request.ts + 6 modules
│       ├── views/               Home, Project, About, Contact + admin/
│       ├── components/          AppHeader, AppFooter, AIChatBubble, ProjectCard
│       └── styles/              variables.css, global.css, ink-effects.css
```
---

## 阶段一：后端基础 (Tasks 1-6)

### Task 1: Spring Boot 项目初始化
- [ ] 创建 pom.xml（Spring Boot 2.7.18, MyBatis-Plus 3.5.5, MySQL Connector, java-jwt 4.4.0, Lombok）
- [ ] 创建 PortfolioApplication.java（@SpringBootApplication + @MapperScan）
- [ ] 创建 application.yml（DS, MyBatis-Plus, openai.api-key, jwt.secret 配置）
- [ ] 验证：`mvn compile -pl portfolio-backend` 编译通过

### Task 2: 数据库建表 schema.sql
- [ ] 创建 `db/schema.sql`，7 张表：user, project, contact_message, skill, timeline_event, knowledge_base, chat_history
- [ ] 含默认管理员 INSERT（密码 BCrypt 加密）
- [ ] 在 MySQL 中执行建表

### Task 3: 通用响应类
- [ ] 创建 `common/Result.java` — `{code, message, data}` + 静态方法 success/error
- [ ] 创建 `common/PageResult.java` — `{records, total, page, pageSize}`

### Task 4: 基础配置类
- [ ] `config/WebMvcConfig.java` — CORS 允许 localhost:5173
- [ ] `config/MyBatisPlusConfig.java` — PaginationInnerInterceptor
- [ ] `config/AIConfig.java` — @ConfigurationProperties("openai") 读取 apiKey/model

### Task 5: 实体类（7个）
- [ ] User, Project, ContactMessage, Skill, TimelineEvent, KnowledgeBase, ChatHistory
- [ ] 全部 @Data + @TableName + @TableId(type=AUTO)，时间字段 @TableField(fill=...)

### Task 6: Mapper 接口（7个）
- [ ] 全部 extends BaseMapper<Entity>，接口 + @Mapper 注解

---

## 阶段二：后端公开 API (Tasks 7-11)

### Task 7: JWT 工具类
- [ ] 创建 `util/JwtUtil.java`
- [ ] generateToken(userId, username) → 用 HMAC256 签名
- [ ] verify(token) → DecodedJWT
- [ ] getUserId(token) → Long

### Task 8: 首页 API — GET /api/home
- [ ] `service/HomeService.java` + `impl/HomeServiceImpl.java`
- [ ] 返回 Map：user(无密码), recentProjects(LIMIT 6), skills, timeline
- [ ] `controller/HomeController.java` → Result.success(homeService.getHomeInfo())

### Task 9: 作品集 API — GET /api/projects, GET /api/projects/{id}
- [ ] `service/ProjectService.java` + impl: page(page, pageSize, tag) / getById(id)
- [ ] 支持分页 + 标签模糊筛选（isPublished=1）
- [ ] `controller/ProjectController.java` → list + detail

### Task 10: 关于我 API — GET /api/about
- [ ] `controller/AboutController.java` — 直接访问 Mapper 返回 user/skills/timeline 聚合数据

### Task 11: 留言 API — POST /api/contact
- [ ] `service/ContactService.java` → submit(ContactMessage)
- [ ] `controller/ContactController.java` → POST 接收 name/email/content

---

## 阶段三：后端管理 API + 安全 (Tasks 12-13)

### Task 12: 登录功能
- [ ] 创建 `dto/LoginDTO.java` — username, password
- [ ] `AdminController.java` — POST /api/admin/login
- [ ] BCryptPasswordEncoder 验证密码 → JWT 生成 → 返回 token+username

### Task 13: 管理后台 CRUD（在 AdminController 中）
- [ ] GET /api/admin/dashboard — 统计：project/skill/message/knowledge 计数 + 未读留言数
- [ ] CRUD /api/admin/projects — 分页列表 / 新增 / 更新 / 删除
- [ ] CRUD /api/admin/skills — 列表 / 新增 / 更新 / 删除
- [ ] CRUD /api/admin/timeline — 列表 / 新增 / 更新 / 删除
- [ ] CRUD /api/admin/knowledge — 分页列表 / 新增 / 更新 / 删除
- [ ] GET /api/admin/messages — 分页列表
- [ ] PUT /api/admin/messages/{id}/read — 标记已读

---

## 阶段四：AI 聊天 (Task 14)

### Task 14: AI 聊天 Service + Controller
- [ ] 创建 `dto/ChatRequestDTO.java` — sessionId, message
- [ ] 创建 `dto/ChatResponseDTO.java` — role, content, sessionId
- [ ] `service/AIChatService.java` → chat(), getHistory(), clearHistory()
- [ ] `impl/AIChatServiceImpl.java`:
  - 生成 sessionId（UUID 前12位）
  - 保存用户消息到 chat_history
  - searchKnowledge(query) — 关键词匹配检索知识库
  - callOpenAI(systemPrompt, message, sessionId) — HttpClient 调用 OpenAI API
  - 系统 Prompt 注入知识库上下文
  - 保存 assistant 回复到 chat_history
- [ ] `controller/AIChatController.java`:
  - POST /api/ai/chat — 发送消息
  - GET /api/ai/chat/{sessionId} — 对话历史
  - DELETE /api/ai/chat/{sessionId} — 清除历史

---

## 阶段五：前端基础搭建 (Tasks 15-19)

### Task 15: Vue 3 项目初始化
- [ ] `npm create vite@latest portfolio-frontend -- --template vue-ts`
- [ ] `npm install vue-router@4 pinia axios`
- [ ] 配置 `vite.config.ts`：@ 别名 + proxy /api → localhost:8080
- [ ] 配置 `tsconfig.json`：paths "@/*": ["src/*"]

### Task 16: 路由配置
- [ ] 创建 `src/router/index.ts`
- [ ] 路由表：/ (Home), /projects, /projects/:id, /about, /contact
- [ ] 管理路由：/admin/login, /admin (Dashboard), /admin/projects, /admin/skills, /admin/timeline, /admin/knowledge, /admin/messages
- [ ] beforeEach 守卫：设置 title，检查 requiresAuth → localStorage token

### Task 17: API 客户端封装
- [ ] `api/request.ts` — axios 实例 + 拦截器（token 注入 + 响应解包）
- [ ] `api/home.ts` — getHomeInfo()
- [ ] `api/project.ts` — getProjects(params), getProjectDetail(id)
- [ ] `api/about.ts` — getAbout()
- [ ] `api/contact.ts` — submitContact(data)
- [ ] `api/ai.ts` — sendMessage, getChatHistory, clearChatHistory
- [ ] `api/admin.ts` — login, getDashboard + 各模块 CRUD

### Task 18: 水墨主题样式
- [ ] `styles/variables.css` — CSS 变量：ink-black, ink-seal(#c43a31), ink-paper, font-serif/sans
- [ ] `styles/global.css` — 全局重置 + .container + .section-title（下划线红印装饰）
- [ ] `styles/ink-effects.css` — 动画：inkSpread, inkDrop, fadeUp, sealRotate

### Task 19: 全局组件 + App 入口
- [ ] `main.ts` — createApp + Pinia + Router + 全局样式导入
- [ ] `App.vue` — AppHeader + router-view(transition) + AppFooter + AIChatBubble
- [ ] `components/AppHeader.vue` — 固定顶部导航 + 滚动阴影 + 移动端汉堡菜单
- [ ] `components/AppFooter.vue` — 印章 + copyright + slogan

---

## 阶段六：前端公开页面 (Tasks 20-22)

### Task 20: 首页 HomeView.vue
- [ ] Hero 区域：山水渐变背景 + 大标题("墨韵之间，见代码") + 简介 + CTA 按钮
- [ ] 近期作品区：ProjectCard 网格（调用 getHomeInfo API）
- [ ] 技术栈区：技能云标签（透明度按 level 变化）
- [ ] 滚动提示箭头（bounce 动画）

### Task 21: 作品集模块
- [ ] `components/ProjectCard.vue`：封面图/占位符 + 标题 + 描述截断 + 标签
- [ ] `views/ProjectView.vue`：标签筛选栏 + 卡片网格 + 加载状态
- [ ] `views/ProjectDetailView.vue`：返回链接 + 封面 + 标题 + 标签 + 详细内容 + 演示/源码链接

### Task 22: 关于我 + 联系方式
- [ ] `views/AboutView.vue`：头像 + 个人简介 + 技能进度条 + 时间线
- [ ] `views/ContactView.vue`：表单（姓名/邮箱/留言）+ 联系信息 + 社交链接 + 发送成功提示

---

## 阶段七：AI 聊天悬浮窗 (Task 23)

### Task 23: AIChatBubble 组件
- [ ] `stores/chat.ts`：isOpen, sessionId, messages, loading → toggle/send/loadHistory/clear
- [ ] `components/AIChatBubble.vue`：
  - 右下角圆形浮动按钮（"砚"字）
  - 点击展开水墨风聊天面板（360x480）
  - 消息列表（用户/助手气泡，不同颜色）
  - 输入区 + 发送按钮
  - 自动滚动到底部
  - transition 缩放弹出动画

---

## 阶段八：管理后台前端 (Task 24)

### Task 24: 管理页面组
- [ ] `views/admin/AdminLogin.vue` — 登录表单，成功后存 token 跳转
- [ ] `views/admin/AdminSidebar.vue` — 侧边导航（仪表盘/作品/技能/时间线/知识库/留言/退出）
- [ ] `views/admin/AdminDashboard.vue` — 4 个统计卡片 + 未读提醒
- [ ] `views/admin/ProjectManage.vue` — 表格列表 + 弹窗表单(新增/编辑) + 删除
- [ ] `views/admin/SkillManage.vue` — 同上模式
- [ ] `views/admin/TimelineManage.vue` — 同上模式
- [ ] `views/admin/KnowledgeManage.vue` — 同上模式
- [ ] `views/admin/MessageManage.vue` — 只读列表 + 标记已读按钮

---

## 自审

- **Spec 覆盖**：全部页面/API/DB 有对应 Task ✓
- **占位符**：无 TBD/TODO ✓
- **类型一致性**：前后端字段名对齐（驼峰命名）✓
