<template>
  <div class="admin-page"><div class="container"><div class="admin-layout">
    <AdminSidebar /><main class="admin-main">
      <div class="page-header"><h1>留言管理</h1></div>
      <table class="data-table">
        <thead><tr><th>ID</th><th>姓名</th><th>邮箱</th><th>内容</th><th>状态</th><th>时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in list" :key="item.id">
          <td>{{ item.id }}</td><td>{{ item.name }}</td><td>{{ item.email }}</td>
          <td>{{ truncate(item.content, 30) }}</td>
          <td><span :class="['status', item.isRead ? 'read' : 'unread']">{{ item.isRead ? '已读' : '未读' }}</span></td>
          <td>{{ formatDate(item.createdAt) }}</td>
          <td><button v-if="!item.isRead" @click="markRead(item.id)">标记已读</button></td>
        </tr></tbody>
      </table>
    </main>
  </div></div></div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getAdminMessages, markMessageRead } from "@/api/admin";
import AdminSidebar from "./AdminSidebar.vue";
const list = ref<any[]>([]);
function truncate(s: string, n: number) { return s && s.length > n ? s.slice(0, n) + "..." : s; }
function formatDate(d: string) { return d ? new Date(d).toLocaleDateString("zh-CN") : ""; }
async function load() { try { const r: any = await getAdminMessages({ page: 1, pageSize: 100 }); list.value = r.data.records || []; } catch (e) {} }
async function markRead(id: number) { try { await markMessageRead(id); load(); } catch (e) {} }
onMounted(load);
</script>

<style scoped>
.admin-page { padding-top: 80px; } .admin-layout { display: flex; gap: 32px; } .admin-main { flex: 1; }
.page-header { margin-bottom: 24px; } .page-header h1 { font-family: var(--font-serif); }
.data-table { width: 100%; border-collapse: collapse; background: var(--ink-white); border: 1px solid var(--ink-border); }
.data-table th, .data-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--ink-border); font-size: 14px; }
.data-table th { background: var(--ink-bg); font-weight: 600; }
.status { padding: 2px 8px; border-radius: 3px; font-size: 12px; }
.status.unread { background: #fef3f2; color: var(--ink-seal); }
.status.read { background: var(--ink-bg); color: var(--ink-light); }
td button { background: none; border: none; color: var(--ink-seal); cursor: pointer; font-size: 13px; }
</style>
