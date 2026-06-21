import request from "./request";

// ==================== Types ====================

export interface AdminProject {
  id?: number;
  title: string;
  summary: string;
  description: string;
  coverUrl: string;
  demoUrl: string;
  githubUrl: string;
  tags: string;
  sortOrder: number;
  isPublished: number;
}

export interface AdminSkill {
  id?: number;
  name: string;
  category: string;
  level: number;
  icon: string;
  sortOrder: number;
}

export interface AdminTimeline {
  id?: number;
  title: string;
  summary: string;
  description: string;
  type: string;
  startDate: string;
  endDate: string;
  sortOrder: number;
}

export interface AdminKnowledge {
  id?: number;
  content: string;
  sourceFile: string;
  sourceHash: string;
  chunkIndex: number;
  category: string;
}

export interface ProfileForm {
  username?: string;
  avatar?: string;
  intro?: string;
}

export interface PasswordForm {
  oldPassword: string;
  newPassword: string;
}

// ==================== Auth ====================

export function login(data: { username: string; password: string }) {
  return request.post("/admin/login", data);
}

// ==================== Profile ====================

export function getProfile() {
  return request.get("/admin/profile");
}

export function updateProfile(data: ProfileForm) {
  return request.put("/admin/profile", data);
}

export function updatePassword(data: PasswordForm) {
  return request.put("/admin/profile/password", data);
}

// ==================== Projects ====================

export function getAdminProjects(params: { page: number; pageSize: number }) {
  return request.get("/admin/projects", { params });
}

export function createProject(data: AdminProject) {
  return request.post("/admin/projects", data);
}

export function updateProject(id: number, data: AdminProject) {
  return request.put(`/admin/projects/${id}`, data);
}

export function deleteProject(id: number) {
  return request.delete(`/admin/projects/${id}`);
}

export function reindexProjects() {
  return request.post("/admin/projects/reindex");
}

// ==================== Skills ====================

export function getAdminSkills() {
  return request.get("/admin/skills");
}

export function createSkill(data: AdminSkill) {
  return request.post("/admin/skills", data);
}

export function updateSkill(id: number, data: AdminSkill) {
  return request.put(`/admin/skills/${id}`, data);
}

export function deleteSkill(id: number) {
  return request.delete(`/admin/skills/${id}`);
}

// ==================== Timeline ====================

export function getAdminTimeline() {
  return request.get("/admin/timeline");
}

export function createTimeline(data: AdminTimeline) {
  return request.post("/admin/timeline", data);
}

export function updateTimeline(id: number, data: AdminTimeline) {
  return request.put(`/admin/timeline/${id}`, data);
}

export function deleteTimeline(id: number) {
  return request.delete(`/admin/timeline/${id}`);
}

// ==================== Knowledge ====================

export function getAdminKnowledge(params: { page: number; pageSize: number }) {
  return request.get("/admin/knowledge", { params });
}

export function createKnowledge(data: AdminKnowledge) {
  return request.post("/admin/knowledge", data);
}

export function updateKnowledge(id: number, data: AdminKnowledge) {
  return request.put(`/admin/knowledge/${id}`, data);
}

export function deleteKnowledge(id: number) {
  return request.delete(`/admin/knowledge/${id}`);
}

export function importKnowledge(file: File): Promise<any> {
  const fd = new FormData();
  fd.append("file", file);
  return request.post("/admin/knowledge/import", fd, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export function deleteKnowledgeFile(sourceHash: string) {
  return request.delete(`/admin/knowledge/file/${sourceHash}`);
}

// ==================== Messages ====================

export function getAdminMessages(params: { page: number; pageSize: number }) {
  return request.get("/admin/messages", { params });
}

export function markMessageRead(id: number) {
  return request.put(`/admin/messages/${id}/read`);
}

// ==================== AI ====================

export function analyzeDocument(file: File): Promise<any> {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/admin/ai/analyze-document", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}