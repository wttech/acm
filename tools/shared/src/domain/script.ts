export const SCRIPT_ROOT = '/conf/acm/settings/script/';

export const SCRIPT_TYPES = ['MANUAL', 'AUTOMATIC', 'EXTENSION', 'MOCK'] as const;

export type ScriptType = (typeof SCRIPT_TYPES)[number];

/** Where a content package keeps the scripts in the repository sources. */
const PACKAGE_SCRIPT_ROOT = '/jcr_root/conf/acm/settings/script';

export function scriptTypeOf(id: string): ScriptType | undefined {
  const type = (id.startsWith(SCRIPT_ROOT) ? id.slice(SCRIPT_ROOT.length) : id).split('/')[0]?.toUpperCase();
  return SCRIPT_TYPES.find((candidate) => candidate === type);
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
