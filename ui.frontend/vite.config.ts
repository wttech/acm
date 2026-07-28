import react from '@vitejs/plugin-react';
import { ClientRequest, IncomingMessage, ServerResponse } from 'node:http';
import path from 'path';
import { defineConfig, HttpProxy, normalizePath, ViteDevServer } from 'vite';
import eslint from 'vite-plugin-eslint'; // TODO fixed by workaround: https://github.com/gxmari007/vite-plugin-eslint/issues/74#issuecomment-1647431890
import { viteStaticCopy } from 'vite-plugin-static-copy';

const devServerPort = 5501;
const aemInstanceTarget = 'http://localhost:5502';
const aemInstanceCredentials = 'admin:admin';
const buildPath = '/apps/acm/gui/spa/build';

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
  base: process.env.NODE_ENV === 'production' ? `${buildPath}/` : '/acm/',
  plugins: [
    react(),
    eslint(),
    // Copy Monaco embeddables
    viteStaticCopy({
      targets: [
        // { src: 'public/*', dest: '../' },
        {
          src: normalizePath(path.join(__dirname, 'node_modules', 'monaco-editor', 'min', 'vs')),
          dest: 'js/monaco-editor',
        },
      ],
    }),
    // Redirect '/acm' to '/acm/' (add trailing slash)
    {
      name: 'redirect-acm-trailing-slash',
      configureServer(server: ViteDevServer) {
        server.middlewares.use((req: IncomingMessage, res: ServerResponse, next: () => void) => {
          if (req.url === '/acm') {
            res.writeHead(301, { Location: '/acm/' });
            res.end();
            return;
          }
          next();
        });
      },
    },
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
    outDir: `../ui.apps/src/main/content/jcr_root${buildPath}`,
    emptyOutDir: true,
    rollupOptions: {
      output: {
        // Use hex hashes so generated names never contain '_' or '-' that FileVault could
        // misread. FileVault decodes on-disk names like '_prefix_local' back into the JCR
        // name 'prefix:local', so a chunk such as '_basePickBy-BqxqY_hh.js' would be imported
        // as an invalid namespaced node and break AEM package installation (OakName0001).
        hashCharacters: 'hex',
        chunkFileNames: (chunkInfo) => {
          // Strip leading underscores (e.g. lodash internals like '_basePickBy') and replace
          // characters that are not safe for JCR/FileVault node names.
          const name = chunkInfo.name.replace(/[:[\]@]/g, '-').replace(/^_+/, '');
          return `chunks/${name}-[hash].js`;
        },
      },
    },
  },
});
