import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const soloDatos = (req: { headers: Record<string, string | string[] | undefined> }) =>
  String(req.headers.accept ?? '').includes('text/html') ? '/index.html' : undefined

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      '/interno': { target: 'http://localhost:8080', bypass: soloDatos },
      '/publico': { target: 'http://localhost:8080', bypass: soloDatos },
    },
  },
})