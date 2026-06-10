<template>
  <footer class="app-footer">
    <div class="container footer-content">
      <div class="seal-stamp">Xinki</div>
      <p>&copy; {{ year }} Xinki. All rights reserved.</p>
      <p class="footer-tagline">墨韵行云 · 代码写意</p>
    </div>
  </footer>
</template>

<script setup lang="ts">
const year = new Date().getFullYear();
</script>

<style scoped>
.app-footer { border-top: 1px solid var(--ink-border); padding: 40px 0; text-align: center; background: var(--ink-paper); }
.footer-content { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.footer-tagline { font-family: var(--font-serif); color: var(--ink-light); font-size: 14px; }
</style>
