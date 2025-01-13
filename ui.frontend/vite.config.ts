import react from '@vitejs/plugin-react';
import { ClientRequest } from 'node:http';
import path from 'path';
import { defineConfig, HttpProxy } from 'vite';
import { viteStaticCopy } from 'vite-plugin-static-copy';

function serverProxyConfig() {
  return {
    target: 'http://localhost:4502',
    changeOrigin: true,
    configure: (proxy: HttpProxy.Server) => {
      proxy.on('proxyReq', (proxyReq: ClientRequest) => {
        proxyReq.setHeader('Authorization', `Basic ${btoa('admin:admin')}`);
        // proxyReq.setHeader('User-Agent', 'curl/8.7.1'); // use it to trick AEM's CSRF Filter
      });
    },
  };
}

export default defineConfig({
  base: process.env.NODE_ENV === 'production' ? '/apps/contentor/spa/' : '/',
  plugins: [
    react(),
    viteStaticCopy({
      targets: [
        // { src: 'public/*', dest: '../' },
        {
          src: path.join(__dirname, 'node_modules', 'monaco-editor', 'min', 'vs'),
          dest: 'js/monaco-editor',
        },
      ],
    }),
  ],
  server: {
    fs: {
      allow: ['node_modules', 'src'],
    },
    proxy: {
      '/apps/contentor/api': serverProxyConfig(),
      '/apps/contentor/spa': serverProxyConfig(),
      '/libs/granite/csrf/token.json': serverProxyConfig(),
    },
  },
  build: {
    outDir: '../ui.apps/src/main/content/jcr_root/apps/contentor/spa',
    emptyOutDir: true,
  },
});
