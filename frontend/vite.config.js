import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The frontend talks to the backend through VITE_API_URL. The default is the
// relative path /api: Vite proxies it to localhost:8080 in development, and in
// production the Spring Boot jar serves both the UI and the API on one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
