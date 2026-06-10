import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: () => import("@/views/HomeView.vue"),
      meta: { title: "首页" },
    },
    {
      path: "/projects",
      name: "projects",
      component: () => import("@/views/ProjectView.vue"),
      meta: { title: "作品集" },
    },
    {
      path: "/projects/:id",
      name: "project-detail",
      component: () => import("@/views/ProjectDetailView.vue"),
      meta: { title: "作品详情" },
    },
    {
      path: "/about",
      name: "about",
      component: () => import("@/views/AboutView.vue"),
      meta: { title: "关于我" },
    },
    {
      path: "/contact",
      name: "contact",
      component: () => import("@/views/ContactView.vue"),
      meta: { title: "联系我" },
    },
    {
      path: "/admin/login",
      name: "admin-login",
      component: () => import("@/views/admin/AdminLogin.vue"),
      meta: { title: "管理员登录" },
    },
    {
      path: "/admin",
      name: "admin-dashboard",
      component: () => import("@/views/admin/AdminDashboard.vue"),
      meta: { title: "管理后台", requiresAuth: true },
    },
    {
      path: "/admin/projects",
      name: "admin-projects",
      component: () => import("@/views/admin/ProjectManage.vue"),
      meta: { title: "作品管理", requiresAuth: true },
    },
    {
      path: "/admin/skills",
      name: "admin-skills",
      component: () => import("@/views/admin/SkillManage.vue"),
      meta: { title: "技能管理", requiresAuth: true },
    },
    {
      path: "/admin/timeline",
      name: "admin-timeline",
      component: () => import("@/views/admin/TimelineManage.vue"),
      meta: { title: "时间线管理", requiresAuth: true },
    },
    {
      path: "/admin/knowledge",
      name: "admin-knowledge",
      component: () => import("@/views/admin/KnowledgeManage.vue"),
      meta: { title: "知识库管理", requiresAuth: true },
    },
    {
      path: "/admin/messages",
      name: "admin-messages",
      component: () => import("@/views/admin/MessageManage.vue"),
      meta: { title: "留言管理", requiresAuth: true },
    },
  ],
});

router.beforeEach((to) => {
  document.title = `${to.meta.title} - Xinki`;
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem("admin_token");
    if (!token) return "/admin/login";
  }
});

export default router;
