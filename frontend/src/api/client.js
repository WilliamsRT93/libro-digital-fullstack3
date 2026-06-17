import axios from "axios";
import { getToken, clearSession } from "../auth/session.js";

// Cliente HTTP centralizado.
// Inyecta el header Authorization en cada request si hay un token y maneja
// expiraciones de sesion (401) limpiando el storage y forzando login.
const client = axios.create({
  baseURL: "/api/v1/bff",
  timeout: 10000
});

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    // Si el token expiro o es invalido, limpiamos sesion y redirigimos a login.
    if (err.response && err.response.status === 401) {
      clearSession();
      if (window.location.pathname !== "/") {
        window.location.assign("/");
      }
    }
    return Promise.reject(err);
  }
);

export default client;
