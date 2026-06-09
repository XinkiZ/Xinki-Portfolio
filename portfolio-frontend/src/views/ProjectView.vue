<template>
  <div class="projects-page">
    <section class="page-section">
      <div class="container">
        <h1 class="section-title">作品集</h1>
        <div class="filter-bar">
          <button
            v-for="tag in allTags"
            :key="tag"
            :class="{ active: activeTag === tag }"
            @click="activeTag = activeTag === tag ? '' : tag"
            class="filter-btn"
          >{{ tag || '全部' }}</button>
        </div>
        <div class="project-grid">
          <ProjectCard v-for="project in projects" :key="project.id" :project="project" />
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-if="!loading && projects.length === 0" class="empty">暂无作品</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { getProjects } from "@/api/project";
import ProjectCard from "@/components/ProjectCard.vue";

const projects = ref<any[]>([]);
const allTags = ref<string[]>([]);
const activeTag = ref("");
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const res: any = await getProjects({ page: 1, pageSize: 50, tag: activeTag.value || undefined });
    projects.value = res.data.records || [];
    if (!activeTag.value) {
      const tagSet = new Set<string>();
      projects.value.forEach((p: any) => {
        if (p.tags) p.tags.split(",").forEach((t: string) => tagSet.add(t.trim()));
      });
      allTags.value = ["全部", ...Array.from(tagSet)];
    }
  } finally {
    loading.value = false;
  }
}

watch(activeTag, load);
onMounted(load);
</script>

<style scoped>
.filter-bar { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-bottom: 40px; }
.filter-btn {
  padding: 6px 20px;
  border: 1px solid var(--ink-border);
  background: none;
  cursor: pointer;
  font-size: 14px;
  border-radius: 4px;
  transition: all var(--transition-ink);
  font-family: var(--font-sans);
}
.filter-btn.active { background: var(--ink-black); color: var(--ink-white); border-color: var(--ink-black); }
.project-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 32px; }
.loading, .empty { text-align: center; color: var(--ink-light); padding: 40px; }
</style>