import { defineStore } from "pinia";
import { ref } from "vue";
import { sendMessageStream, getChatHistory, clearChatHistory } from "@/api/ai";

interface Message {
  role: "user" | "assistant";
  content: string;
}

export const useChatStore = defineStore("chat", () => {
  const isOpen = ref(false);
  const sessionId = ref(localStorage.getItem("ai_session") || "");
  const messages = ref<Message[]>([]);
  const loading = ref(false);
  /** Streaming partial content (shown while assistant is typing) */
  const streamingContent = ref("");

  function toggle() { isOpen.value = !isOpen.value; }

  async function send(msg: string) {
    if (!msg.trim() || loading.value) return;

    messages.value.push({ role: "user", content: msg });
    loading.value = true;
    streamingContent.value = "";

    // Push a placeholder for the assistant message
    messages.value.push({ role: "assistant", content: "" });
    const assistantIndex = messages.value.length - 1;

    sendMessageStream(
      { sessionId: sessionId.value, message: msg },
      // onChunk
      (token: string) => {
        streamingContent.value += token;
        messages.value[assistantIndex].content = streamingContent.value;
      },
      // onDone
      (newSessionId: string) => {
        sessionId.value = newSessionId;
        localStorage.setItem("ai_session", newSessionId);
        loading.value = false;
        streamingContent.value = "";
      },
      // onError
      (errMsg: string) => {
        messages.value[assistantIndex].content = "抱歉，出了点问题...";
        loading.value = false;
        streamingContent.value = "";
        console.error("Chat stream error:", errMsg);
      }
    );
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

  return { isOpen, sessionId, messages, loading, streamingContent, toggle, send, loadHistory, clear };
});