import request from "./request";
export function login(data: { username: string; password: string }) { return request.post("/admin/login", data); }
export function getDashboard() { return request.get("/admin/dashboard"); }
export function getAdminProjects(params: { page: number; pageSize: number }) { return request.get("/admin/projects", { params }); }
export function createProject(data: any) { return request.post("/admin/projects", data); }
export function updateProject(id: number, data: any) { return request.put(`/admin/projects/${id}`, data); }
export function deleteProject(id: number) { return request.delete(`/admin/projects/${id}`); }
export function getAdminSkills() { return request.get("/admin/skills"); }
export function createSkill(data: any) { return request.post("/admin/skills", data); }
export function updateSkill(id: number, data: any) { return request.put(`/admin/skills/${id}`, data); }
export function deleteSkill(id: number) { return request.delete(`/admin/skills/${id}`); }
export function getAdminTimeline() { return request.get("/admin/timeline"); }
export function createTimeline(data: any) { return request.post("/admin/timeline", data); }
export function updateTimeline(id: number, data: any) { return request.put(`/admin/timeline/${id}`, data); }
export function deleteTimeline(id: number) { return request.delete(`/admin/timeline/${id}`); }
export function getAdminKnowledge(params: { page: number; pageSize: number }) { return request.get("/admin/knowledge", { params }); }
export function createKnowledge(data: any) { return request.post("/admin/knowledge", data); }
export function updateKnowledge(id: number, data: any) { return request.put(`/admin/knowledge/${id}`, data); }
export function deleteKnowledge(id: number) { return request.delete(`/admin/knowledge/${id}`); }
export function getAdminMessages(params: { page: number; pageSize: number }) { return request.get("/admin/messages", { params }); }
export function markMessageRead(id: number) { return request.put(`/admin/messages/${id}/read`); }

// AI 文档分析
export function analyzeDocument(file: File): Promise<any> {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/admin/ai/analyze-document", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}