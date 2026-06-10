<template>
  <AppHeader />
  <main>
    <router-view v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </main>
  <AppFooter />
  <AIChatBubble />
</template>

<script setup lang="ts">
import AppHeader from "@/components/AppHeader.vue";
import AppFooter from "@/components/AppFooter.vue";
import AIChatBubble from "@/components/AIChatBubble.vue";
</script>

<style>
.page-fade-enter-active, .page-fade-leave-active { transition: opacity 0.3s ease; }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; }
</style>
