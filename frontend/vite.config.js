import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Modules call relative '/api/v1/...' paths. Proxy them to the Spring Boot backend so
    // the browser stays same-origin and the API needs no CORS configuration.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // The header health indicator polls the actuator, which is served outside /api.
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
