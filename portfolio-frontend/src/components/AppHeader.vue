<template>
  <header class="app-header" :class="{ scrolled: isScrolled }">
    <div class="container header-inner">
      <router-link to="/" class="logo">
        <span class="logo-text">墨·Xinki</span>
      </router-link>
      <nav class="nav-links">
        <router-link to="/" exact-active-class="active">首页</router-link>
        <router-link to="/projects" active-class="active">作品集</router-link>
        <router-link to="/about" active-class="active">关于我</router-link>
        <router-link to="/contact" active-class="active">联系我</router-link>
      </nav>
      <button class="mobile-toggle" @click="menuOpen = !menuOpen">
        <span></span><span></span><span></span>
      </button>
    </div>
    <transition name="slide">
      <nav v-if="menuOpen" class="mobile-nav">
        <router-link to="/" @click="menuOpen = false">首页</router-link>
        <router-link to="/projects" @click="menuOpen = false">作品集</router-link>
        <router-link to="/about" @click="menuOpen = false">关于我</router-link>
        <router-link to="/contact" @click="menuOpen = false">联系我</router-link>
      </nav>
    </transition>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
const isScrolled = ref(false);
const menuOpen = ref(false);
const handleScroll = () => { isScrolled.value = window.scrollY > 50; };
onMounted(() => window.addEventListener("scroll", handleScroll));
onUnmounted(() => window.removeEventListener("scroll", handleScroll));
</script>

<style scoped>
.app-header {
  position: fixed; top: 0; left: 0; right: 0; z-index: 1000;
  background: rgba(250, 246, 240, 0.85); backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--ink-border); transition: all var(--transition-ink);
}
.app-header.scrolled { box-shadow: var(--shadow-ink); }
.header-inner { display: flex; justify-content: space-between; align-items: center; height: 64px; }
.logo-text { font-family: var(--font-serif); font-size: 24px; letter-spacing: 4px; }
.nav-links { display: flex; gap: 32px; }
.nav-links a { font-size: 15px; padding: 4px 0; border-bottom: 1px solid transparent; transition: border-color var(--transition-ink); }
.nav-links a.active, .nav-links a:hover { border-bottom-color: var(--ink-seal); }
.mobile-toggle { display: none; flex-direction: column; gap: 5px; background: none; border: none; cursor: pointer; }
.mobile-toggle span { width: 24px; height: 2px; background: var(--ink-black); }
.mobile-nav { padding: 16px 24px; background: var(--ink-paper); display: flex; flex-direction: column; gap: 12px; border-top: 1px solid var(--ink-border); }
@media (max-width: 768px) { .nav-links { display: none; } .mobile-toggle { display: flex; } }
.slide-enter-active, .slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
