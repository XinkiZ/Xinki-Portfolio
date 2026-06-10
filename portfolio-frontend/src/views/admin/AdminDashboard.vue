<template>
  <div class="admin-page">
    <div class="container"><div class="admin-layout">
      <AdminSidebar />
      <main class="admin-main">
        <h1>仪表盘</h1>
        <div class="stats-grid">
          <div class="stat-card"><span class="stat-num">{{ data?.projectCount || 0 }}</span><span class="stat-label">作品</span></div>
          <div class="stat-card"><span class="stat-num">{{ data?.skillCount || 0 }}</span><span class="stat-label">技能</span></div>
          <div class="stat-card"><span class="stat-num">{{ data?.messageCount || 0 }}</span><span class="stat-label">留言</span></div>
          <div class="stat-card"><span class="stat-num">{{ data?.knowledgeCount || 0 }}</span><span class="stat-label">知识库</span></div>
        </div>
        <div v-if="data?.unreadMessages" class="alert">你有 {{ data.unreadMessages }} 条未读留言</div>
      </main>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getDashboard } from "@/api/admin";
import AdminSidebar from "./AdminSidebar.vue";
const data = ref<any>(null);
onMounted(async () => { try { const res: any = await getDashboard(); data.value = res.data; } catch (e) {} });
</script>

<style scoped>
.admin-page { padding-top: 80px; }
.admin-layout { display: flex; gap: 32px; }
.admin-main { flex: 1; }
.admin-main h1 { font-family: var(--font-serif); margin-bottom: 24px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: var(--ink-white); padding: 24px; text-align: center; border: 1px solid var(--ink-border); border-radius: 4px; }
.stat-num { display: block; font-family: var(--font-serif); font-size: 32px; }
.stat-label { font-size: 14px; color: var(--ink-light); }
.alert { padding: 12px 16px; background: #fef3f2; border: 1px solid var(--ink-seal-light); border-radius: 4px; color: var(--ink-seal); }
@media (max-width: 768px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
