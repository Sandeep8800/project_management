import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// API Design S2.1: backend serves under /api/v1 -- proxied in dev so the SPA
// can call relative paths without a CORS dance.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/v1": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
