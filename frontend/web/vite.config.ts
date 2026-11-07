import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => ({
  base: process.env.VITE_BASE_PATH || (mode === "demo" ? "/RentalTrack/" : "/"),
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.RENTALTRACK_API_URL || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
}));
