import api from '../../skills/acm-groovy-script/references/api.md';
import patterns from '../../skills/acm-groovy-script/references/patterns.md';
import scripts from '../../skills/acm-groovy-script/references/scripts.md';
import skill from '../../skills/acm-groovy-script/SKILL.md';

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
    description: 'Script types, conditions, schedules, inputs, outputs, logging, aborting, locking and extension scripts.',
    text: scripts,
  },
  {
    path: 'references/patterns.md',
    title: 'ACM script patterns',
    description: 'Templates for content migration, ACL setup, CSV reports and scheduled cleanup.',
    text: patterns,
  },
];

/** The skill's core rules, short enough to send to an agent up front. */
export const SKILL_ESSENTIALS = section(skill, 'Essentials');
