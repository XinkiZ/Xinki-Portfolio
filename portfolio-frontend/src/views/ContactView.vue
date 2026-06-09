<template>
  <div class="contact-page">
    <section class="page-section">
      <div class="container">
        <h1 class="section-title">联系我</h1>
        <div class="contact-grid">
          <div class="contact-form-card fade-up">
            <form @submit.prevent="submitForm">
              <div class="form-group">
                <label>姓名</label>
                <input v-model="form.name" type="text" required placeholder="你的名字" />
              </div>
              <div class="form-group">
                <label>邮箱</label>
                <input v-model="form.email" type="email" required placeholder="your@email.com" />
              </div>
              <div class="form-group">
                <label>留言</label>
                <textarea v-model="form.content" rows="6" required placeholder="写下你想说的话..."></textarea>
              </div>
              <button type="submit" class="btn-primary" :disabled="submitting">
                {{ submitting ? '发送中...' : '发送留言' }}
              </button>
            </form>
            <p v-if="sent" class="success-msg seal-stamp">留言已发送 ✓</p>
          </div>
          <div class="contact-info fade-up" style="animation-delay: 0.2s">
            <div class="info-item"><span class="info-icon">✉</span><span>email@xinki.dev</span></div>
            <div class="info-item"><span class="info-icon">⌂</span><span>中国 · 在线</span></div>
            <div class="social-links">
              <a href="#" class="social-link">GitHub</a>
              <a href="#" class="social-link">掘金</a>
              <a href="#" class="social-link">知乎</a>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { submitContact } from "@/api/contact";

const form = reactive({ name: "", email: "", content: "" });
const submitting = ref(false);
const sent = ref(false);

async function submitForm() {
  submitting.value = true;
  try {
    await submitContact({ ...form });
    sent.value = true;
    form.name = form.email = form.content = "";
  } catch (e) { console.error(e); }
  finally { submitting.value = false; }
}
</script>

<style scoped>
.contact-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 48px; max-width: 900px; margin: 0 auto; }
@media (max-width: 768px) { .contact-grid { grid-template-columns: 1fr; } }
.contact-form-card { background: var(--ink-white); padding: 40px; border: 1px solid var(--ink-border); border-radius: 4px; }
.form-group { margin-bottom: 24px; }
.form-group label { display: block; font-size: 14px; margin-bottom: 8px; font-family: var(--font-serif); }
.form-group input, .form-group textarea {
  width: 100%; padding: 12px; border: 1px solid var(--ink-border);
  border-radius: 4px; font-family: var(--font-sans); font-size: 15px;
  background: var(--ink-paper); transition: border-color var(--transition-ink);
}
.form-group input:focus, .form-group textarea:focus { outline: none; border-color: var(--ink-black); }
.btn-primary { display: block; width: 100%; padding: 14px; font-size: 16px; background: var(--ink-black); color: var(--ink-white); border: none; border-radius: 4px; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.success-msg { text-align: center; margin-top: 16px; font-size: 14px; }
.contact-info { display: flex; flex-direction: column; gap: 24px; justify-content: center; }
.info-item { display: flex; align-items: center; gap: 12px; font-size: 15px; }
.info-icon { font-size: 20px; }
.social-links { display: flex; gap: 20px; margin-top: 12px; }
.social-link { font-family: var(--font-serif); border-bottom: 1px solid var(--ink-border); padding-bottom: 2px; }
</style>