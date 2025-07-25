import react from '@vitejs/plugin-react';
import { ClientRequest } from 'node:http';
import path from 'path';
import { defineConfig, HttpProxy, normalizePath } from 'vite';
import eslint from 'vite-plugin-eslint'; // TODO fixed by workaround: https://github.com/gxmari007/vite-plugin-eslint/issues/74#issuecomment-1647431890
import { viteStaticCopy } from 'vite-plugin-static-copy';

const devServerPort = 5501;
const aemInstanceTarget = 'http://localhost:5502';
const aemInstanceCredentials = 'admin:admin';

function serverProxyConfig() {
  return {
    target: aemInstanceTarget,
    changeOrigin: true,
    configure: (proxy: HttpProxy.Server) => {
      proxy.on('proxyReq', (proxyReq: ClientRequest) => {
        proxyReq.setHeader('Authorization', `Basic ${btoa(aemInstanceCredentials)}`);
        proxyReq.setHeader('Origin', aemInstanceTarget); // use it to trick AEM's CSRF Filter
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
          src: normalizePath(path.join(__dirname, 'node_modules', 'monaco-editor', 'min', 'vs')),
          dest: 'js/monaco-editor',
        },
      ],
    }),
  ],
  server: {
    strictPort: true,
    port: devServerPort,
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
