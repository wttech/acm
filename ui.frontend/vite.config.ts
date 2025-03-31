import react from '@vitejs/plugin-react';
import { ClientRequest } from 'node:http';
import path from 'path';
import { defineConfig, HttpProxy } from 'vite';
import eslint from 'vite-plugin-eslint'; // TODO fixed by workaround: https://github.com/gxmari007/vite-plugin-eslint/issues/74#issuecomment-1647431890
import { viteStaticCopy } from 'vite-plugin-static-copy';

function serverProxyConfig() {
  return {
    target: 'http://localhost:4502',
    changeOrigin: true,
    configure: (proxy: HttpProxy.Server) => {
      proxy.on('proxyReq', (proxyReq: ClientRequest) => {
        proxyReq.setHeader('Authorization', `Basic ${btoa('admin:admin')}`);
        proxyReq.setHeader('Origin', 'http://localhost:4502'); // use it to trick AEM's CSRF Filter
      });
    },
  };
}

export default defineConfig({
  base: process.env.NODE_ENV === 'production' ? '/apps/acm/spa/' : '/',
  plugins: [
    react(),
    eslint(),
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
    strictPort: true,
    port: 5173,
    fs: {
      allow: ['node_modules', 'src'],
    },
    proxy: {
      '/apps/acm/api': serverProxyConfig(),
      '/apps/acm/spa': serverProxyConfig(),
      '/libs/granite/csrf/token.json': serverProxyConfig(),
    },
  },
  build: {
    outDir: '../ui.apps/src/main/content/jcr_root/apps/acm/spa',
    emptyOutDir: true,
  },
});
