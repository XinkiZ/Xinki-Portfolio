import request from "./request";
export function getHomeInfo() { return request.get("/home"); }
