import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import path from 'path';

export default defineConfig({
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
    }
  },
  build: {
    outDir: '../ui.apps/src/main/content/jcr_root/apps/migrator/spa',
    emptyOutDir: true,
  }
});
