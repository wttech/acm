export class Strings {
  static dedent(text: string): string {
    const lines = text.split('\n');
    // Find minimum indentation (ignoring empty lines)
    const minIndent = lines
      .filter((line) => line.trim().length > 0)
      .reduce((min, line) => {
        const indent = line.match(/^(\s*)/)?.[1].length || 0;
        return Math.min(min, indent);
      }, Infinity);

    // Remove common leading whitespace
    return lines.map((line) => line.slice(minIndent === Infinity ? 0 : minIndent)).join('\n');
  }
}