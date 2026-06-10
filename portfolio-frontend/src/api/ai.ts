import request from "./request";
export function sendMessage(data: { sessionId: string; message: string }) { return request.post("/ai/chat", data); }
export function getChatHistory(sessionId: string) { return request.get(`/ai/chat/${sessionId}`); }
export function clearChatHistory(sessionId: string) { return request.delete(`/ai/chat/${sessionId}`); }
export function generateProjectContent(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/ai/chat/generate-content", formData);
}