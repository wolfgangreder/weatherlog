import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/META-INF/resources',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/weather': 'http://localhost:8080',
    },
  },
})
