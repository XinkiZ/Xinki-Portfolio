<template>
  <div class="about-page">
    <section class="page-section">
      <div class="container">
        <h1 class="section-title">关于我</h1>

        <div v-if="data?.user" class="intro-card fade-up">
          <div class="avatar-circle">
            <img v-if="data.user.avatar" :src="data.user.avatar" alt="avatar" />
            <span v-else class="avatar-text">墨</span>
          </div>
          <p class="intro-text">{{ data.user.intro || '一个热爱技术与艺术的开发者。' }}</p>
        </div>

        <div v-if="data?.skills?.length" class="skills-block fade-up">
          <h2>技术栈</h2>
          <div class="skills-list">
            <div v-for="skill in data.skills" :key="skill.id" class="skill-bar">
              <span class="skill-name">{{ skill.name }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: skill.level + '%' }"></div></div>
            </div>
          </div>
        </div>

        <div v-if="data?.timeline?.length" class="timeline-block fade-up">
          <h2>经历</h2>
          <div class="timeline">
            <div v-for="event in data.timeline" :key="event.id" class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <span class="event-date">{{ formatDate(event.startDate) }} — {{ event.endDate ? formatDate(event.endDate) : '至今' }}</span>
                <h3>{{ event.title }}</h3>
                <p>{{ event.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getAbout } from "@/api/about";

const data = ref<any>(null);

function formatDate(dateStr: string) {
  if (!dateStr) return "";
  return dateStr.split("-")[0];
}

onMounted(async () => {
  try {
    const res: any = await getAbout();
    data.value = res.data;
  } catch (e) { console.error(e); }
});
</script>

<style scoped>
.intro-card { text-align: center; max-width: 600px; margin: 0 auto 80px; }
.avatar-circle {
  width: 120px; height: 120px; border-radius: 50%;
  margin: 0 auto 24px; border: 2px solid var(--ink-border);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.avatar-circle img { width: 100%; height: 100%; object-fit: cover; }
.avatar-text { font-family: var(--font-serif); font-size: 48px; color: var(--ink-light); }
.intro-text { font-size: 16px; color: var(--ink-gray); line-height: 2; }
.skills-block, .timeline-block { max-width: 700px; margin: 0 auto 80px; }
.skills-block h2, .timeline-block h2 { font-family: var(--font-serif); font-size: 24px; margin-bottom: 32px; text-align: center; }
.skill-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.skill-name { width: 100px; text-align: right; font-size: 14px; }
.bar-track { flex: 1; height: 4px; background: var(--ink-bg); border-radius: 2px; overflow: hidden; }
.bar-fill { height: 100%; background: var(--ink-black); transition: width 1s ease; }
.timeline { position: relative; padding-left: 24px; }
.timeline::before { content: ""; position: absolute; left: 4px; top: 0; bottom: 0; width: 1px; background: var(--ink-border); }
.timeline-item { position: relative; margin-bottom: 32px; }
.timeline-dot { position: absolute; left: -20px; top: 6px; width: 9px; height: 9px; border-radius: 50%; background: var(--ink-seal); }
.event-date { font-size: 13px; color: var(--ink-light); }
.timeline-content h3 { font-family: var(--font-serif); font-size: 18px; margin: 4px 0; }
.timeline-content p { font-size: 14px; color: var(--ink-gray); }
</style>