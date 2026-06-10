import request from "./request";
export function getProjects(params: { page?: number; pageSize?: number; tag?: string }) { return request.get("/projects", { params }); }
export function getProjectDetail(id: number) { return request.get(`/projects/${id}`); }
