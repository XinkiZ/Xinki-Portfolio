import request from "./request";
export function submitContact(data: { name: string; email: string; content: string }) { return request.post("/contact", data); }
