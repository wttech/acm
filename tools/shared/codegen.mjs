#!/usr/bin/env node
// Generates the ACM Groovy script API catalog from the Java sources in core/ (no Java needed).
// Run after changing the script API: node tools/shared/codegen.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '../..');
const JAVA = join(ROOT, 'core/src/main/java');
const CODE = 'dev.vml.es.acm.core.code';
const OUT_JSON = join(ROOT, 'tools/shared/src/catalog/api.json');
const OUT_MD = join(ROOT, 'tools/skills/acm-groovy-script/references/api.md');
const MAX_DEPTH = 2;

const cache = new Map();

function sourceFile(fqn) {
  return join(JAVA, `${fqn.replaceAll('.', '/')}.java`);
}

function baseType(type) {
  return type.replace(/<.*$/s, '').replace(/\[\]|\.\.\./g, '').trim();
}

function resolveType(type, cls) {
  const simple = baseType(type);
  if (!simple || !/^[A-Z]/.test(simple)) return null;
  const explicit = cls.imports.find((i) => !i.wildcard && i.name.endsWith(`.${simple}`));
  if (explicit) return explicit.name;
  const candidates = [cls.pkg, ...cls.imports.filter((i) => i.wildcard).map((i) => i.name)];
  for (const pkg of candidates) {
    if (existsSync(sourceFile(`${pkg}.${simple}`))) return `${pkg}.${simple}`;
  }
  return null;
}

function splitParams(raw) {
  const parts = [];
  let depth = 0;
  let current = '';
  for (const ch of raw) {
    if (ch === '<') depth++;
    if (ch === '>') depth--;
    if (ch === ',' && depth === 0) {
      parts.push(current);
      current = '';
    } else {
      current += ch;
    }
  }
  parts.push(current);
  return parts
    .map((p) => p.replace(/@\w+\s*/g, '').replace(/\bfinal\s+/g, '').replace(/\s+/g, ' ').trim())
    .filter(Boolean)
    .map((p) => {
      const i = p.lastIndexOf(' ');
      return { type: p.slice(0, i), name: p.slice(i + 1) };
    });
}

function readPreamble(lines) {
  let deprecated = false;
  let doc;
  for (let i = lines.length - 1; i >= 0; i--) {
    const line = lines[i].trim();
    if (line.startsWith('@')) {
      deprecated ||= line.startsWith('@Deprecated');
    } else if (line.endsWith('*/')) {
      const start = lines.slice(0, i + 1).findLastIndex((l) => l.trim().startsWith('/**'));
      if (start < 0) break;
      doc = lines
        .slice(start, i + 1)
        .map((l) => l.trim().replace(/^\/\*\*|\*\/$|^\*/g, '').trim())
        .filter((l) => l && !l.startsWith('@'))
        .join(' ');
      break;
    } else {
      break;
    }
  }
  return { deprecated, doc };
}

// Public instance methods declared at class-member indentation (4 spaces); interface methods are implicitly public.
const METHOD =
  /^ {4}public\s+(?!static\b|class\b|enum\b|interface\b)(?:(?:final|synchronized|abstract)\s+)*(?:<[^()]*?>\s+)?([\w.<>[\]?, ]+?)\s+(\w+)\s*\(([^)]*)\)/gm;
const INTERFACE_METHOD =
  /^ {4}(?:public\s+)?(?:default\s+)?(?!static\b|class\b|enum\b|interface\b|return\b)(?:<[^()]*?>\s+)?([\w.<>[\]?, ]+?)\s+(\w+)\s*\(([^)]*)\)/gm;
const ENUM_CONSTANT = /^ {4}([A-Z][A-Z0-9_]*)\s*(?:\(|,|;|$)/gm;

function parseClass(fqn) {
  if (cache.has(fqn)) return cache.get(fqn);
  if (!fqn || !existsSync(sourceFile(fqn))) return null;
  const src = readFileSync(sourceFile(fqn), 'utf8');
  const name = fqn.split('.').pop();
  const cls = {
    fqn,
    name,
    src,
    pkg: /^package\s+([\w.]+);/m.exec(src)[1],
    imports: [...src.matchAll(/^import\s+([\w.]+?)(\.\*)?;/gm)].map((m) => ({ name: m[1], wildcard: !!m[2] })),
    methods: [],
  };
  cache.set(fqn, cls);
  const kind = new RegExp(`\\b(class|interface|enum)\\s+${name}\\b`).exec(src)?.[1] ?? 'class';
  const superName = new RegExp(`(?:class|interface)\\s+${name}(?:<[^{]*?>)?\\s+extends\\s+(\\w+)`).exec(src)?.[1];
  cls.superclass = superName ? resolveType(superName, cls) : null;
  if (kind === 'enum') cls.values = [...src.matchAll(ENUM_CONSTANT)].map((m) => m[1]);
  for (const m of src.matchAll(kind === 'interface' ? INTERFACE_METHOD : METHOD)) {
    const [, returns, methodName, params] = m;
    const { deprecated, doc } = readPreamble(src.slice(0, m.index).split('\n').slice(0, -1));
    cls.methods.push({
      name: methodName,
      returns: returns.trim(),
      params: splitParams(params),
      ...(deprecated && { deprecated }),
      ...(doc && { doc }),
    });
  }
  return cls;
}

function allMethods(cls) {
  const own = cls.methods;
  const inherited = cls.superclass ? allMethods(parseClass(cls.superclass) ?? { methods: [] }) : [];
  const key = (m) => `${m.name}/${m.params.length}`;
  const ownKeys = new Set(own.map(key));
  return [...own, ...inherited.filter((m) => !ownKeys.has(key(m)))];
}

function closureType(param, cls) {
  const inner = /^Closure<(.+)>$/.exec(param.type)?.[1];
  return inner ? resolveType(inner, cls) : null;
}

function fieldType(cls, field) {
  return new RegExp(`^\\s+private\\s+(?:final\\s+)?(?:transient\\s+)?([\\w.<>?, ]+?)\\s+${field}\\s*[;=]`, 'm').exec(
    cls.src,
  )?.[1];
}

function parseBindings() {
  const bindings = new Map();
  for (const fqn of [`${CODE}.CodeContext`, `${CODE}.ExecutionContext`]) {
    const cls = parseClass(fqn);
    for (const [, name, expr, call, rest] of cls.src.matchAll(/setVariable\("(\w+)",\s*([\w.]+)(\(\))?\);(.*)$/gm)) {
      const type =
        expr === 'this' ? cls.name : call ? cls.methods.find((m) => m.name === expr)?.returns : fieldType(cls, expr);
      bindings.set(name, {
        name,
        type,
        class: resolveType(type, cls),
        ...(/deprecated/i.test(rest) && { deprecated: true }),
      });
    }
  }
  return [...bindings.values()].sort((a, b) => a.name.localeCompare(b.name));
}

function parseLifecycle(fqn) {
  const cls = parseClass(fqn);
  return [...cls.src.matchAll(/^\s+[A-Z_]+\("(\w+)",\s*([^,]+),\s*(true|false)(?:,\s*(\d+))?\)/gm)].map(
    ([, name, returns, required, paramCount]) => ({
      name,
      returns: returns.replace(/\.class\.getName\(\)$/, '').replaceAll('"', ''),
      required: required === 'true',
      paramCount: Number(paramCount ?? 0),
    }),
  );
}

// Mock methods are required per mock type; the regular mock needs the ones its `isMethodRequired` names.
function parseMockLifecycle(fqn) {
  const cls = parseClass(fqn);
  const required = [...(/case REGULAR:\s*return ([^;]+);/.exec(cls.src)?.[1] ?? '').matchAll(/Method\.(\w+)/g)].map((m) => m[1]);
  return [...cls.src.matchAll(/^\s+([A-Z_]+)\("(\w+)",\s*"([\w.]+)",\s*(\d+)\)/gm)].map(([, constant, name, returns, paramCount]) => ({
    name,
    returns,
    required: required.includes(constant),
    paramCount: Number(paramCount),
  }));
}

// Script DSL methods are the ones taking a configuration closure, e.g. inputs.string('name') { ... }.
function parseDsl(fqn) {
  const cls = parseClass(fqn);
  const result = new Map();
  for (const m of cls.methods) {
    const options = m.params.map((p) => closureType(p, cls)).find(Boolean);
    if (options) {
      result.set(m.name, {
        name: m.name,
        returns: m.returns,
        options,
        optionsRequired: !cls.methods.some((o) => o.name === m.name && o.params.length === 1),
        ...(m.deprecated && { deprecated: true }),
      });
    }
  }
  return [...result.values()];
}

function collectClasses(roots) {
  const classes = new Map();
  const queue = roots.filter(Boolean).map((fqn) => ({ fqn, depth: 0 }));
  while (queue.length) {
    const { fqn, depth } = queue.shift();
    if (classes.has(fqn)) continue;
    const cls = parseClass(fqn);
    if (!cls) continue;
    const methods = allMethods(cls);
    classes.set(fqn, {
      name: cls.name,
      ...(cls.superclass && { superclass: cls.superclass }),
      ...(cls.values && { values: cls.values }),
      methods,
    });
    if (depth >= MAX_DEPTH) continue;
    for (const m of methods) {
      const owner = parseClass(fqn);
      const refs = [resolveType(m.returns, owner), ...m.params.map((p) => closureType(p, owner))];
      for (const ref of refs) if (ref && !classes.has(ref)) queue.push({ fqn: ref, depth: depth + 1 });
    }
  }
  return Object.fromEntries([...classes.entries()].sort(([a], [b]) => a.localeCompare(b)));
}

const bindings = parseBindings();
const inputs = parseDsl(`${CODE}.Inputs`);
const outputs = parseDsl(`${CODE}.Outputs`);
const api = {
  lifecycle: {
    content: parseLifecycle(`${CODE}.script.ContentScriptSyntax`),
    extension: parseLifecycle(`${CODE}.script.ExtensionScriptSyntax`),
    mock: parseMockLifecycle(`${CODE}.script.MockScriptSyntax`),
  },
  bindings,
  inputs,
  outputs,
  classes: collectClasses([
    ...bindings.map((b) => b.class),
    ...inputs.map((i) => i.options),
    ...outputs.map((o) => o.options),
  ]),
};

function signature(m) {
  return `${m.returns} ${m.name}(${m.params.map((p) => `${p.type} ${p.name}`).join(', ')})`;
}

function markdown() {
  const lines = [
    '# ACM Groovy API reference',
    '',
    '<!-- Generated by tools/shared/codegen.mjs from core/src/main/java. Do not edit. -->',
    '',
    'Public API available to ACM Groovy scripts, extracted from the ACM source code.',
    'Getters can be used as Groovy properties, e.g. `context.id` for `context.getId()`.',
    '',
    '## Script methods',
    '',
    '| Script type | Method | Required |',
    '|---|---|---|',
    ...Object.entries(api.lifecycle).flatMap(([type, methods]) =>
      methods.map((m) => {
        const params = m.paramCount ? '…' : '';
        return `| ${type} | \`${m.returns.split('.').pop()} ${m.name}(${params})\` | ${m.required ? 'yes' : 'no'} |`;
      }),
    ),
    '',
    '## Variables',
    '',
    '| Variable | Type |',
    '|---|---|',
    ...bindings.map((b) => `| \`${b.name}\` | \`${b.type}\`${b.deprecated ? ' (deprecated)' : ''} |`),
    '',
    '## Inputs',
    '',
    'Declare in `describeRun()`, read in `doRun()` with `inputs.value(\'name\')`. Options are set in the closure.',
    '',
    '| Declaration | Options type |',
    '|---|---|',
    ...inputs.map(
      (i) =>
        `| \`inputs.${i.name}(String name${i.optionsRequired ? ', Closure options' : ', Closure options = null'})\` | \`${i.options.split('.').pop()}\` |`,
    ),
    '',
    '## Outputs',
    '',
    '| Declaration | Options type |',
    '|---|---|',
    ...outputs.map(
      (o) =>
        `| \`outputs.${o.name}(String name, Closure options = null)\` | \`${o.options.split('.').pop()}\`${o.deprecated ? ' (deprecated)' : ''} |`,
    ),
    '',
    '## Classes',
  ];
  for (const [fqn, cls] of Object.entries(api.classes)) {
    lines.push('', `### ${cls.name}`, '', `\`${fqn}\`${cls.superclass ? `, extends \`${cls.superclass.split('.').pop()}\`` : ''}`);
    if (cls.values) lines.push('', `Values: ${cls.values.map((v) => `\`${v}\``).join(', ')}`);
    if (!cls.methods.length) continue;
    lines.push('', '```groovy');
    for (const m of cls.methods) {
      lines.push(`${signature(m)}${m.deprecated ? ' // deprecated' : ''}${m.doc ? ` // ${m.doc}` : ''}`);
    }
    lines.push('```');
  }
  return `${lines.join('\n')}\n`;
}

writeFileSync(OUT_JSON, `${JSON.stringify(api, null, 2)}\n`);
writeFileSync(OUT_MD, markdown());
console.log(
  `ACM API catalog: ${bindings.length} variables, ${inputs.length} inputs, ${outputs.length} outputs, ${Object.keys(api.classes).length} classes.`,
);
