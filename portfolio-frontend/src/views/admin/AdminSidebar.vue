<template>
  <aside class="admin-sidebar">
    <nav>
      <router-link to="/admin">仪表盘</router-link>
      <router-link to="/admin/profile">个人设置</router-link>
      <router-link to="/admin/projects">作品管理</router-link>
      <router-link to="/admin/skills">技能管理</router-link>
      <router-link to="/admin/timeline">时间线</router-link>
      <router-link to="/admin/knowledge">知识库</router-link>
      <router-link to="/admin/ai-assistant">AI 助手</router-link>
      <router-link to="/admin/messages">留言管理</router-link>
      <a href="#" @click.prevent="logout">退出登录</a>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
const router = useRouter();
function logout() { localStorage.removeItem("admin_token"); router.push("/admin/login"); }
</script>

<style scoped>
.admin-sidebar { width: 180px; flex-shrink: 0; border-right: 1px solid var(--ink-border); padding-right: 24px; }
.admin-sidebar nav { display: flex; flex-direction: column; gap: 8px; }
.admin-sidebar a { padding: 8px 12px; border-radius: 4px; font-size: 14px; }
.admin-sidebar a.router-link-active { background: var(--ink-black); color: var(--ink-white); }
</style>