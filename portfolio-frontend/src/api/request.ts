import axios from "axios";

const request = axios.create({
  baseURL: "/api",
  timeout: 60000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("admin_token");
  if (token && config.url?.startsWith("/admin")) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    console.error("API Error:", err);
    return Promise.reject(err);
  }
);

export default request;
