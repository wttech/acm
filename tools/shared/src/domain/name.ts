/** Removes the extension from a file name when it has it. */
export function stripExtension(name: string, extension: string): string {
  return name.endsWith(extension) ? name.slice(0, -extension.length) : name;
}

/** Checks a new file name given as a path under its folder, e.g. `example/ACME-1_hello`; `undefined` when valid. */
export function validateRelativeName(name: string, extension: string): string | undefined {
  const base = stripExtension(name.trim(), extension);
  if (base === '') {
    return 'Enter a name.';
  }
  if (base.startsWith('/') || base.endsWith('/')) {
    return 'Use a path relative to the folder, e.g. example/hello.';
  }
  if (!/^[\w.\-/]+$/.test(base) || base.split('/').some((part) => part === '' || part === '.' || part === '..')) {
    return 'Use letters, digits, dots, dashes, underscores and slashes only.';
  }
  return undefined;
}
