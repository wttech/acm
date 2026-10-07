// Fails when the identifiers in src/ids.ts and the contributions in package.json drift apart.
import { readFileSync } from 'node:fs';
import { transform } from 'esbuild';

const manifest = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'));
const source = readFileSync(new URL('../src/ids.ts', import.meta.url), 'utf8');
const { code } = await transform(source, { loader: 'ts', format: 'esm' });
const ids = await import(`data:text/javascript;base64,${Buffer.from(code).toString('base64')}`);

const contributes = manifest.contributes;
const problems = [];

function same(label, fromCode, fromManifest) {
  const code = new Set(fromCode);
  const declared = new Set(fromManifest);
  for (const value of code) {
    if (!declared.has(value)) problems.push(`${label}: "${value}" is in src/ids.ts but not in package.json`);
  }
  for (const value of declared) {
    if (!code.has(value)) problems.push(`${label}: "${value}" is in package.json but not in src/ids.ts`);
  }
}

same('commands', Object.values(ids.COMMANDS), contributes.commands.map((command) => command.command));
same('views', Object.values(ids.VIEWS), Object.values(contributes.views).flat().map((view) => view.id));
same(
  'settings',
  Object.values(ids.SETTINGS).map(ids.settingId),
  Object.keys(contributes.configuration.properties),
);
same('mcp providers', [ids.MCP_PROVIDER_ID], contributes.mcpServerDefinitionProviders.map((provider) => provider.id));

for (const [key, property] of Object.entries(contributes.configuration.properties)) {
  if (property.default === undefined) problems.push(`settings: "${key}" has no default; the code relies on it`);
}

const executionExtension = contributes.languages.flatMap((language) => language.extensions ?? []);
if (!executionExtension.includes(ids.DOCUMENTS.executionExtension)) {
  problems.push(`languages: no language handles ${ids.DOCUMENTS.executionExtension} execution documents`);
}

// Every `when` clause may only read views, context keys and settings that the code defines.
const contextKeys = new Set([...Object.values(ids.CONTEXT), ...Object.values(ids.VIEWS)]);
const menus = Object.values(contributes.menus).flat();
for (const clause of [...menus, ...Object.values(contributes.views).flat()].map((item) => item.when).filter(Boolean)) {
  for (const key of clause.match(/\bacm\.[A-Za-z.]+/g) ?? []) {
    if (!contextKeys.has(key)) problems.push(`when: "${key}" in "${clause}" is not a context key or view in src/ids.ts`);
  }
}

// Menu items may only reference declared commands.
for (const item of menus) {
  if (!contributes.commands.some((command) => command.command === item.command)) {
    problems.push(`menus: "${item.command}" is not a declared command`);
  }
}

if (problems.length > 0) {
  console.error(problems.map((problem) => `- ${problem}`).join('\n'));
  process.exit(1);
}
console.log('package.json matches src/ids.ts');
