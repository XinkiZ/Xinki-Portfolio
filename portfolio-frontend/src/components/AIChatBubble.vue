<template>
  <div class="ai-chat-wrapper">
    <button class="ai-bubble-btn" @click="chat.toggle()" :class="{ active: chat.isOpen }">
      <span class="bubble-icon">砚</span>
      <span v-if="!chat.isOpen" class="bubble-hint">AI 助手</span>
    </button>

    <transition name="chat-slide">
      <div v-if="chat.isOpen" class="chat-panel">
        <div class="chat-header">
          <span class="chat-title">砚·墨问</span>
          <div class="chat-header-actions">
            <button @click="chat.clear()" class="btn-clear">清除</button>
            <button @click="chat.toggle()" class="btn-close">×</button>
          </div>
        </div>

        <div class="chat-messages" ref="msgContainer">
          <div v-for="(msg, i) in chat.messages" :key="i" :class="['msg', msg.role]">
            <div class="msg-avatar">{{ msg.role === 'user' ? '我' : '砚' }}</div>
            <div class="msg-content">{{ msg.content }}</div>
          </div>
          <div v-if="chat.loading" class="msg assistant">
            <div class="msg-avatar">砚</div>
            <div class="msg-content typing">···</div>
          </div>
        </div>

        <div class="chat-input-area">
          <input v-model="input" @keyup.enter="sendMsg" placeholder="问点什么..." :disabled="chat.loading" />
          <button @click="sendMsg" :disabled="chat.loading">发送</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from "vue";
import { useChatStore } from "@/stores/chat";

const chat = useChatStore();
const input = ref("");
const msgContainer = ref<HTMLElement | null>(null);

watch(() => chat.isOpen, (val) => { if (val) chat.loadHistory(); });

watch(() => chat.messages.length, async () => {
  await nextTick();
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight;
});

function sendMsg() {
  if (!input.value.trim()) return;
  chat.send(input.value);
  input.value = "";
}
</script>

<style scoped>
.ai-chat-wrapper { position: fixed; bottom: 24px; right: 24px; z-index: 9999; }
.ai-bubble-btn {
  width: 60px; height: 60px; border-radius: 50%;
  border: 2px solid var(--ink-border); background: var(--ink-paper);
  cursor: pointer; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  font-family: var(--font-serif); font-size: 24px;
  transition: all var(--transition-ink); box-shadow: var(--shadow-ink); position: relative;
}
.ai-bubble-btn:hover { border-color: var(--ink-black); transform: scale(1.05); }
.ai-bubble-btn.active { background: var(--ink-black); color: var(--ink-white); border-color: var(--ink-black); }
.bubble-hint { font-family: var(--font-sans); font-size: 10px; position: absolute; bottom: -20px; white-space: nowrap; color: var(--ink-light); }
.chat-panel {
  position: absolute; bottom: 80px; right: 0; width: 360px; height: 480px;
  background: var(--ink-paper); border: 1px solid var(--ink-border);
  border-radius: 8px; box-shadow: 0 8px 40px rgba(0,0,0,0.12);
  display: flex; flex-direction: column; overflow: hidden;
}
.chat-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--ink-border); }
.chat-title { font-family: var(--font-serif); font-size: 16px; }
.chat-header-actions { display: flex; gap: 8px; }
.btn-clear, .btn-close { background: none; border: none; cursor: pointer; font-size: 13px; color: var(--ink-light); }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.msg { display: flex; gap: 8px; }
.msg.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 28px; height: 28px; border-radius: 50%; background: var(--ink-bg);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-family: var(--font-serif); flex-shrink: 0;
}
.msg-content { max-width: 240px; padding: 10px 14px; border-radius: 8px; font-size: 14px; line-height: 1.6; }
.msg.user .msg-content { background: var(--ink-black); color: var(--ink-white); }
.msg.assistant .msg-content { background: var(--ink-white); border: 1px solid var(--ink-border); }
.typing { color: var(--ink-light); }
.chat-input-area { display: flex; padding: 12px; border-top: 1px solid var(--ink-border); gap: 8px; }
.chat-input-area input {
  flex: 1; padding: 10px; border: 1px solid var(--ink-border);
  border-radius: 4px; font-size: 14px; background: var(--ink-white);
}
.chat-input-area input:focus { outline: none; border-color: var(--ink-black); }
.chat-input-area button {
  padding: 10px 16px; background: var(--ink-black); color: var(--ink-white);
  border: none; border-radius: 4px; cursor: pointer; font-size: 14px;
}
.chat-input-area button:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-slide-enter-active, .chat-slide-leave-active { transition: all 0.3s ease; }
.chat-slide-enter-from, .chat-slide-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }
</style>
