import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import path from 'path';

export default defineConfig({
  base: process.env.NODE_ENV === 'production' ? '/apps/migrator/spa/' : '/',
  plugins: [
    react(),
    viteStaticCopy({
      targets: [
        // { src: 'public/*', dest: '../' },
        { src: path.join(__dirname, 'node_modules', 'monaco-editor', 'min', 'vs'), dest: 'js/monaco-editor' },
      ],
    }),
  ],
  server: {
    fs: {
      allow: [
        'node_modules',
        'src',
      ],
    },
    proxy: {
      '/apps/migrator/api': {
        target: 'http://localhost:4502',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Authorization', `Basic ${btoa('admin:admin')}`);
            proxyReq.setHeader('User-Agent', 'curl/8.7.1'); // trick AEM's filter 'com.adobe.granite.csrf.impl.CSRFFilter'
          });
        }
      }
    }
  },
  build: {
    outDir: '../ui.apps/src/main/content/jcr_root/apps/migrator/spa',
    emptyOutDir: true,
  }
});
