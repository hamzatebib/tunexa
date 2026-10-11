import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // En développement, les appels /api sont redirigés vers le backend Spring Boot
    proxy: {
      '/api': {
        target: process.env.API_URL ?? 'http://localhost:8080',
        // Le proxy rend l'appel « même origine » : inutile de transmettre l'en-tête Origin (contrôle CORS)
        configure: (proxy) => proxy.on('proxyReq', (req) => req.removeHeader('origin')),
      },
    },
  },
})
