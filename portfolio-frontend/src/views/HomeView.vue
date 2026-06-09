<template>
  <div class="home-page">
    <section class="hero">
      <div class="ink-wash-bg"></div>
      <div class="container hero-content">
        <h1 class="hero-title ink-drop">
          <span class="title-line">墨韵之间</span>
          <span class="title-line">见代码</span>
        </h1>
        <p class="hero-intro fade-up" style="animation-delay: 0.3s">
          {{ homeData?.user?.intro || '全栈开发者 / 热爱技术与水墨' }}
        </p>
        <div class="hero-actions fade-up" style="animation-delay: 0.5s">
          <router-link to="/projects" class="btn-primary">查看作品</router-link>
          <router-link to="/contact" class="btn-ghost">联系我</router-link>
        </div>
      </div>
      <div class="scroll-hint">↓</div>
    </section>

    <section class="page-section">
      <div class="container">
        <h2 class="section-title">近期作品</h2>
        <div class="project-grid">
          <ProjectCard v-for="project in homeData?.recentProjects" :key="project.id" :project="project" />
        </div>
        <div class="see-more">
          <router-link to="/projects" class="btn-ghost">查看全部 →</router-link>
        </div>
      </div>
    </section>

    <section class="page-section skills-section">
      <div class="container">
        <h2 class="section-title">技术栈</h2>
        <div class="skills-cloud">
          <span v-for="skill in homeData?.skills" :key="skill.id" class="skill-tag" :style="{ opacity: skill.level / 100 + 0.3 }">
            {{ skill.name }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getHomeInfo } from "@/api/home";
import ProjectCard from "@/components/ProjectCard.vue";

const homeData = ref<any>(null);

onMounted(async () => {
  try {
    const res: any = await getHomeInfo();
    homeData.value = res.data;
  } catch (e) {
    console.error("Failed to load home data:", e);
  }
});
</script>

<style scoped>
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  overflow: hidden;
}
.ink-wash-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, var(--ink-paper) 0%, var(--ink-bg) 50%, rgba(0,0,0,0.03) 100%);
}
.ink-wash-bg::after {
  content: "";
  position: absolute;
  top: 20%;
  left: 10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(0,0,0,0.04) 0%, transparent 70%);
}
.hero-content {
  text-align: center;
  position: relative;
  z-index: 1;
}
.hero-title {
  font-family: var(--font-serif);
  font-size: clamp(36px, 8vw, 72px);
  line-height: 1.3;
  margin-bottom: 24px;
}
.title-line { display: block; }
.hero-intro {
  font-size: 18px;
  color: var(--ink-gray);
  max-width: 500px;
  margin: 0 auto 40px;
}
.hero-actions { display: flex; gap: 16px; justify-content: center; }
.btn-primary {
  background: var(--ink-black);
  color: var(--ink-white);
  padding: 12px 32px;
  border-radius: 4px;
  font-size: 15px;
  transition: all var(--transition-ink);
}
.btn-primary:hover { background: var(--ink-seal); color: var(--ink-white); }
.btn-ghost {
  border: 1px solid var(--ink-black);
  padding: 12px 32px;
  border-radius: 4px;
  font-size: 15px;
  transition: all var(--transition-ink);
}
.btn-ghost:hover { background: var(--ink-black); color: var(--ink-white); }
.scroll-hint {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 24px;
  color: var(--ink-light);
  animation: bounce 2s infinite;
}
@keyframes bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(8px); }
}
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 32px;
}
.see-more { text-align: center; margin-top: 40px; }
.skills-section { background: var(--ink-paper); }
.skills-cloud { display: flex; flex-wrap: wrap; gap: 12px; justify-content: center; }
.skill-tag {
  padding: 8px 20px;
  background: var(--ink-white);
  border: 1px solid var(--ink-border);
  border-radius: 4px;
  font-size: 14px;
  font-family: var(--font-serif);
}
</style>