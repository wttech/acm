export const SCRIPT_ROOT = '/conf/acm/settings/script/';

export const SCRIPT_TYPES = ['MANUAL', 'AUTOMATIC', 'EXTENSION', 'MOCK'] as const;

export type ScriptType = (typeof SCRIPT_TYPES)[number];

/** What a user sees of a script type: its name and what scripts of it are for. */
export const SCRIPT_TYPE_INFO: Record<ScriptType, { label: string; description: string }> = {
  MANUAL: {
    label: 'Manual',
    description: 'Run on demand by a user, often with inputs. For migrations, reports and one-off fixes.',
  },
  AUTOMATIC: {
    label: 'Automatic',
    description: 'Run by ACM on instance boot or on a schedule. For setup after each deployment and maintenance.',
  },
  EXTENSION: {
    label: 'Extension',
    description: 'Hooks into every execution to add variables to scripts or react to their results.',
  },
  MOCK: {
    label: 'Mock',
    description: 'Answers HTTP requests to /mock/* to simulate a third-party service.',
  },
};

/** Optional ACM features whose script types stay hidden while the feature is off. */
export interface ScriptFeatures {
  mock: boolean;
}

export function enabledScriptTypes(features: ScriptFeatures): ScriptType[] {
  return SCRIPT_TYPES.filter((type) => type !== 'MOCK' || features.mock);
}

/** Where a content package keeps the scripts in the repository sources. */
export const PACKAGE_SCRIPT_ROOT = '/jcr_root/conf/acm/settings/script';

export function scriptTypeOf(id: string): ScriptType | undefined {
  const type = (id.startsWith(SCRIPT_ROOT) ? id.slice(SCRIPT_ROOT.length) : id).split('/')[0]?.toUpperCase();
  return SCRIPT_TYPES.find((candidate) => candidate === type);
}

/** The type of a script given the path of its file or its repository ID; `undefined` outside the scripts root. */
export function scriptTypeOfPath(filePath: string): ScriptType | undefined {
  const normalized = filePath.replace(/\\/g, '/');
  const index = normalized.lastIndexOf(SCRIPT_ROOT);
  return index < 0 ? undefined : scriptTypeOf(normalized.slice(index + SCRIPT_ROOT.length));
}

/** The name ACM shows for a script: its path under the type folder, without the extension. */
export function scriptLabel(id: string): string {
  const relative = id.startsWith(SCRIPT_ROOT) ? id.slice(SCRIPT_ROOT.length) : id;
  const withoutType = scriptTypeOf(id) ? relative.slice(relative.indexOf('/') + 1) : relative;
  return withoutType.replace(/\.groovy$/, '');
}

/** The repository ID of a script given its path under the scripts root, e.g. `manual/example/a.groovy`. */
export function scriptIdOf(relativePath: string): string {
  return SCRIPT_ROOT + relativePath.replace(/\\/g, '/').replace(/^\/+/, '');
}

/** Distinct scripts roots of a content package found among file paths; each root ends with `/jcr_root/conf/acm/settings/script`. */
export function scriptRootsOf(filePaths: string[]): string[] {
  const roots = new Set<string>();
  for (const filePath of filePaths) {
    const normalized = filePath.replace(/\\/g, '/');
    const index = normalized.indexOf(`${PACKAGE_SCRIPT_ROOT}/`);
    if (index >= 0) {
      roots.add(normalized.slice(0, index + PACKAGE_SCRIPT_ROOT.length));
    }
  }
  return [...roots].sort();
}

/** Short labels telling several scripts roots apart: the path segments they do not share, at least one each. */
export function scriptRootLabels(roots: string[]): string[] {
  if (roots.length === 0) {
    return [];
  }
  const parts = roots.map((root) => {
    const normalized = root.replace(/\\/g, '/');
    const base = normalized.endsWith(PACKAGE_SCRIPT_ROOT) ? normalized.slice(0, -PACKAGE_SCRIPT_ROOT.length) : normalized;
    return base.split('/').filter(Boolean);
  });
  const shortest = Math.min(...parts.map((segments) => segments.length));
  const [first] = parts;
  let head = 0;
  while (head < shortest - 1 && parts.every((segments) => segments[head] === first[head])) {
    head++;
  }
  let tail = 0;
  while (
    head + tail < shortest - 1 &&
    parts.every((segments) => segments[segments.length - 1 - tail] === first[first.length - 1 - tail])
  ) {
    tail++;
  }
  return parts.map((segments) => segments.slice(head, segments.length - tail).join('/'));
}

/** Checks a new script name given as a path under its type folder, e.g. `example/ACME-1_hello`; `undefined` when valid. */
export function validateScriptName(name: string): string | undefined {
  const base = name.trim().replace(/\.groovy$/, '');
  if (base === '') {
    return 'Enter a name.';
  }
  if (base.startsWith('/') || base.endsWith('/')) {
    return 'Use a path relative to the type folder, e.g. example/hello.';
  }
  if (!/^[\w.\-/]+$/.test(base) || base.split('/').some((part) => part === '' || part === '.' || part === '..')) {
    return 'Use letters, digits, dots, dashes, underscores and slashes only.';
  }
  return undefined;
}
