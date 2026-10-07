import * as esbuild from 'esbuild';

// '@acm/shared' is resolved from tsconfig.json paths and bundled in; npm dependencies stay external.
await esbuild.build({
  entryPoints: ['src/index.ts'],
  bundle: true,
  format: 'esm',
  platform: 'node',
  target: 'node22',
  outfile: 'dist/index.js',
  external: ['@modelcontextprotocol/sdk', 'zod'],
  loader: { '.md': 'text', '.groovy': 'text' },
  logLevel: 'info',
});
