import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
    // ↑ Tells Vite to replace any reference to `global` with `globalThis`
    //   during the build. `globalThis` works in both browsers and Node.js.
    //   SockJS references `global` internally — this makes it work in Vite.
  },
})