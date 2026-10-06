import * as esbuild from 'esbuild';
import { cpSync, rmSync } from 'node:fs';
import { resolve } from 'node:path';

const production = process.argv.includes('--production');
const watch = process.argv.includes('--watch');

// Contributed to Copilot via `chatSkills`; evals are for skill development only.
rmSync('skills', { recursive: true, force: true });
cpSync('../skills/acm-groovy-script', 'skills/acm-groovy-script', {
  recursive: true,
  filter: (source) => !source.includes('/evals'),
});

// '@acm/shared' is resolved from tsconfig.json paths and bundled in.
const ctx = await esbuild.context({
  entryPoints: ['src/extension.ts'],
  bundle: true,
  format: 'cjs',
  platform: 'node',
  target: 'node20',
  outfile: 'dist/extension.js',
  external: ['vscode'],
  loader: { '.md': 'text' },
  minify: production,
  sourcemap: !production,
  sourcesContent: false,
  logLevel: 'info',
});

// The MCP server registered by the extension, with its npm dependencies bundled in (resolved from this package).
const mcpCtx = await esbuild.context({
  entryPoints: ['../mcp-server/src/index.ts'],
  tsconfig: '../mcp-server/tsconfig.json',
  bundle: true,
  format: 'esm',
  platform: 'node',
  target: 'node20',
  outfile: 'dist/mcp-server.mjs',
  nodePaths: [resolve('node_modules')],
  loader: { '.md': 'text' },
  // Bundled CommonJS dependencies may call require().
  banner: { js: "import { createRequire } from 'node:module'; const require = createRequire(import.meta.url);" },
  minify: production,
  logLevel: 'info',
});

if (watch) {
  await Promise.all([ctx.watch(), mcpCtx.watch()]);
} else {
  await Promise.all([ctx.rebuild(), mcpCtx.rebuild()]);
  await Promise.all([ctx.dispose(), mcpCtx.dispose()]);
}
