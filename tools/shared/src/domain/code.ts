/** Wrap bare Groovy in the canRun/doRun contract if the user didn't. */
export function normalizeGroovy(code: string): string {
  if (/\bvoid\s+doRun\s*\(/.test(code)) return code;
  const indented = code
    .split('\n')
    .map((l) => (l.trim() ? '    ' + l : l))
    .join('\n');
  return `boolean canRun() {\n    return conditions.always()\n}\n\nvoid doRun() {\n${indented}\n}`;
}
