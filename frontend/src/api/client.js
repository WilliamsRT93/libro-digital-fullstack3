import axios from "axios";
import { getToken, clearSession } from "../auth/session.js";

// Cliente HTTP centralizado.
// En Next.js el proxy /api/v1/bff/* hacia el BFF se configura en next.config.js (rewrites).
const client = axios.create({
  baseURL: "/api/v1/bff",
  timeout: 10000,
});

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      clearSession();
      if (typeof window !== "undefined" && window.location.pathname !== "/") {
        window.location.assign("/");
      }
    }
    return Promise.reject(err);
  }
);

export default client;
