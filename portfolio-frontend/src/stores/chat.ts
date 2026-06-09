import { defineStore } from "pinia";
import { ref } from "vue";
import { sendMessage as apiSend, getChatHistory, clearChatHistory } from "@/api/ai";

interface Message {
  role: "user" | "assistant";
  content: string;
}

export const useChatStore = defineStore("chat", () => {
  const isOpen = ref(false);
  const sessionId = ref(localStorage.getItem("ai_session") || "");
  const messages = ref<Message[]>([]);
  const loading = ref(false);

  function toggle() { isOpen.value = !isOpen.value; }

  async function send(msg: string) {
    if (!msg.trim()) return;
    messages.value.push({ role: "user", content: msg });
    loading.value = true;
    try {
      const res: any = await apiSend({ sessionId: sessionId.value, message: msg });
      sessionId.value = res.data.sessionId;
      localStorage.setItem("ai_session", sessionId.value);
      messages.value.push({ role: "assistant", content: res.data.content });
    } catch (e) {
      messages.value.push({ role: "assistant", content: "抱歉，出了点问题..." });
    } finally { loading.value = false; }
  }

  async function loadHistory() {
    if (!sessionId.value) return;
    try {
      const res: any = await getChatHistory(sessionId.value);
      messages.value = res.data || [];
    } catch (e) {}
  }

  async function clear() {
    if (sessionId.value) await clearChatHistory(sessionId.value);
    messages.value = [];
    sessionId.value = "";
    localStorage.removeItem("ai_session");
  }

  return { isOpen, sessionId, messages, loading, toggle, send, loadHistory, clear };
});
