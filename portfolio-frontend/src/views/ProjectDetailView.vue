<template>
  <div class="project-detail-page">
    <section class="page-section">
      <div class="container">
        <router-link to="/projects" class="back-link">← 返回作品集</router-link>
        <div v-if="project" class="detail-content fade-up">
          <div class="detail-cover">
            <img v-if="project.coverUrl" :src="project.coverUrl" :alt="project.title" />
          </div>
          <h1>{{ project.title }}</h1>
          <div class="detail-tags">
            <span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <div class="detail-body" v-html="project.description"></div>
          <div class="detail-links">
            <a v-if="project.demoUrl" :href="project.demoUrl" target="_blank" class="btn-primary">在线演示</a>
            <a v-if="project.githubUrl" :href="project.githubUrl" target="_blank" class="btn-ghost">源码</a>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getProjectDetail } from "@/api/project";

const route = useRoute();
const project = ref<any>(null);

const tags = computed(() => {
  if (!project.value?.tags) return [];
  return project.value.tags.split(",").filter(Boolean);
});

onMounted(async () => {
  try {
    const res: any = await getProjectDetail(Number(route.params.id));
    project.value = res.data;
  } catch (e) { console.error(e); }
});
</script>

<style scoped>
.back-link { display: inline-block; margin-bottom: 32px; color: var(--ink-gray); }
.detail-content h1 { font-family: var(--font-serif); font-size: 32px; margin: 24px 0 16px; }
.detail-tags { display: flex; gap: 8px; margin-bottom: 32px; }
.tag { padding: 4px 14px; background: var(--ink-bg); border-radius: 2px; font-size: 13px; }
.detail-body { font-size: 16px; line-height: 2; max-width: 800px; }
.detail-links { display: flex; gap: 16px; margin-top: 32px; }
</style>