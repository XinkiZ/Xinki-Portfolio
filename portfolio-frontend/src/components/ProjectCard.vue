<template>
  <router-link :to="`/projects/${project.id}`" class="project-card fade-up">
    <div class="card-image">
      <img v-if="project.coverUrl" :src="project.coverUrl" :alt="project.title" />
      <div v-else class="card-placeholder">墨</div>
    </div>
    <div class="card-body">
      <h3>{{ project.title }}</h3>
      <p>{{ truncate(project.description, 80) }}</p>
      <div class="card-tags">
        <span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{ project: any }>();

const tags = computed(() => {
  if (!props.project.tags) return [];
  return props.project.tags.split(",").filter(Boolean);
});

function truncate(text: string, len: number) {
  if (!text) return "";
  return text.length > len ? text.slice(0, len) + "..." : text;
}
</script>

<style scoped>
.project-card {
  display: block;
  background: var(--ink-white);
  border: 1px solid var(--ink-border);
  border-radius: 4px;
  overflow: hidden;
  transition: all var(--transition-ink);
}
.project-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-ink); }
.card-image {
  aspect-ratio: 16/10;
  background: var(--ink-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.card-image img { width: 100%; height: 100%; object-fit: cover; }
.card-placeholder { font-family: var(--font-serif); font-size: 48px; color: var(--ink-light); }
.card-body { padding: 20px; }
.card-body h3 { font-family: var(--font-serif); font-size: 18px; margin-bottom: 8px; }
.card-body p { font-size: 14px; color: var(--ink-gray); margin-bottom: 12px; }
.card-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { font-size: 12px; padding: 2px 10px; background: var(--ink-bg); border-radius: 2px; color: var(--ink-gray); }
</style>