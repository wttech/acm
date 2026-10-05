import * as esbuild from 'esbuild';
import { cpSync, rmSync } from 'node:fs';

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

if (watch) {
  await ctx.watch();
} else {
  await ctx.rebuild();
  await ctx.dispose();
}
