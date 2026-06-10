import request from "./request";

export function sendMessage(data: { sessionId: string; message: string }) {
  return request.post("/ai/chat", data);
}

export function getChatHistory(sessionId: string) {
  return request.get(`/ai/chat/${sessionId}`);
}

export function clearChatHistory(sessionId: string) {
  return request.delete(`/ai/chat/${sessionId}`);
}

export function generateProjectContent(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/ai/chat/generate-content", formData);
}

/**
 * Streaming chat via SSE.
 * @param data  { sessionId, message }
 * @param onChunk  called with each token as it arrives
 * @param onDone   called with final sessionId when complete
 * @param onError  called on error
 * @returns AbortController to cancel
 */
export function sendMessageStream(
  data: { sessionId: string; message: string },
  onChunk: (token: string) => void,
  onDone: (sessionId: string) => void,
  onError: (message: string) => void
): AbortController {
  const controller = new AbortController();

  fetch("/api/ai/chat/stream", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        onError(`HTTP ${response.status}`);
        return;
      }
      const reader = response.body?.getReader();
      if (!reader) {
        onError("浏览器不支持流式读取");
        return;
      }
      const decoder = new TextDecoder();
      let buffer = "";
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        // Parse SSE frames: "event:xxx\ndata:{...}\n\n"
        const parts = buffer.split("\n\n");
        buffer = parts.pop() || "";
        for (const part of parts) {
          const lines = part.split("\n");
          let eventType = "";
          let eventData = "";
          for (const line of lines) {
            if (line.startsWith("event:")) eventType = line.substring(6).trim();
            else if (line.startsWith("data:")) eventData = line.substring(5).trim();
          }
          if (!eventData) continue;
          try {
            const parsed = JSON.parse(eventData);
            if (parsed.type === "chunk") {
              onChunk(parsed.content);
            } else if (parsed.type === "done") {
              onDone(parsed.sessionId);
            } else if (parsed.type === "error") {
              onError(parsed.message || "未知错误");
            }
          } catch {
            // Skip unparseable frames
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== "AbortError") {
        onError(err.message || "网络错误");
      }
    });

  return controller;
}