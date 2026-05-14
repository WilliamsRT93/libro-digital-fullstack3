import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Configuracion de Vite para desarrollo y build.
// Hace proxy de /bff hacia el BFF local durante el desarrollo.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      "/bff": "http://localhost:8080"
    }
  }
});
