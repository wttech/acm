import api from '../../skills/acm-groovy-script/references/api.md';
import scripts from '../../skills/acm-groovy-script/references/scripts.md';
import skill from '../../skills/acm-groovy-script/SKILL.md';
import automaticMinimal from '../../skills/acm-groovy-script/templates/automatic/minimal.groovy';
import automaticPermissions from '../../skills/acm-groovy-script/templates/automatic/permissions.groovy';
import automaticScheduledCleanup from '../../skills/acm-groovy-script/templates/automatic/scheduled-cleanup.groovy';
import consoleQuickCheck from '../../skills/acm-groovy-script/templates/console/quick-check.groovy';
import extensionHooks from '../../skills/acm-groovy-script/templates/extension/hooks.groovy';
import manualContentMigration from '../../skills/acm-groovy-script/templates/manual/content-migration.groovy';
import manualMinimal from '../../skills/acm-groovy-script/templates/manual/minimal.groovy';
import manualPermissions from '../../skills/acm-groovy-script/templates/manual/permissions.groovy';
import manualReportCsv from '../../skills/acm-groovy-script/templates/manual/report-csv.groovy';
import mockHttpEndpoint from '../../skills/acm-groovy-script/templates/mock/http-endpoint.groovy';
import { SCRIPT_TYPES } from './domain/script';

export const SKILL_NAME = 'acm-groovy-script';

export interface SkillDocument {
  /** Path relative to the skill folder; relative links in the documents resolve against it. */
  path: string;
  title: string;
  description: string;
  text: string;
}

function stripFrontmatter(markdown: string): string {
  return markdown.replace(/^---\n[\s\S]*?\n---\n+/, '');
}

function section(markdown: string, heading: string): string {
  const start = markdown.indexOf(`\n## ${heading}\n`);
  if (start < 0) return '';
  const end = markdown.indexOf('\n## ', start + 1);
  return markdown.slice(start + 1, end < 0 ? undefined : end).trim();
}

export const SKILL_DOCUMENTS: SkillDocument[] = [
  {
    path: 'SKILL.md',
    title: 'ACM Groovy scripts',
    description: 'How to write, validate and run ACM Groovy scripts safely.',
    text: stripFrontmatter(skill),
  },
  {
    path: 'references/api.md',
    title: 'ACM Groovy API reference',
    description: 'Every variable, class and method available to ACM scripts, generated from the ACM source.',
    text: api,
  },
  {
    path: 'references/scripts.md',
    title: 'ACM scripts',
    description: 'Script types, conditions, schedules, inputs, outputs, logging, aborting, locking, documentation, extension and mock scripts, snippets.',
    text: scripts,
  },
];

/** The skill's core rules, short enough to send to an agent up front. */
export const SKILL_ESSENTIALS = section(skill, 'Essentials');

/** What a template is written for: a script type, or bare code pasted into the console. */
export const TEMPLATE_TARGETS = [...SCRIPT_TYPES, 'CONSOLE'] as const;

export type TemplateTarget = (typeof TEMPLATE_TARGETS)[number];

export interface ScriptTemplate {
  /** Path relative to the skill folder: `templates/{target}/{name}.groovy`. */
  path: string;
  target: TemplateTarget;
  name: string;
  description: string;
  code: string;
}

// esbuild cannot import a folder, so every template file is listed here.
const TEMPLATE_FILES: Record<string, string> = {
  'templates/manual/minimal.groovy': manualMinimal,
  'templates/manual/content-migration.groovy': manualContentMigration,
  'templates/manual/permissions.groovy': manualPermissions,
  'templates/manual/report-csv.groovy': manualReportCsv,
  'templates/automatic/minimal.groovy': automaticMinimal,
  'templates/automatic/permissions.groovy': automaticPermissions,
  'templates/automatic/scheduled-cleanup.groovy': automaticScheduledCleanup,
  'templates/extension/hooks.groovy': extensionHooks,
  'templates/mock/http-endpoint.groovy': mockHttpEndpoint,
  'templates/console/quick-check.groovy': consoleQuickCheck,
};

function templateOf(path: string, code: string): ScriptTemplate {
  const [, folder = '', file = ''] = path.split('/');
  const target = TEMPLATE_TARGETS.find((candidate) => candidate === folder.toUpperCase());
  if (!target) {
    throw new Error(`Template ${path} is in an unknown folder '${folder}'.`);
  }
  const header = /^\/\*([\s\S]*?)\*\//.exec(code)?.[1]?.replace(/^\s*---\n[\s\S]*?\n---\n/, '').trim();
  if (!header) {
    throw new Error(`Template ${path} must start with a documentation header.`);
  }
  const words = file.replace(/\.groovy$/, '').replace(/-/g, ' ');
  return {
    path,
    target,
    name: words.charAt(0).toUpperCase() + words.slice(1),
    description: (header.split('\n\n')[0] ?? header).replace(/\n/g, ' '),
    code,
  };
}

export const SCRIPT_TEMPLATES: ScriptTemplate[] = Object.entries(TEMPLATE_FILES).map(([path, code]) =>
  templateOf(path, code),
);

export function templatesFor(target: TemplateTarget): ScriptTemplate[] {
  return SCRIPT_TEMPLATES.filter((template) => template.target === target);
}

for (const target of TEMPLATE_TARGETS) {
  if (templatesFor(target).length === 0) {
    throw new Error(`No template for ${target} in templates/.`);
  }
}
