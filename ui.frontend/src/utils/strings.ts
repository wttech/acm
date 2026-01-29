export class Strings {
  static notBlank(text: string | null | undefined): boolean {
    return !!text && text.trim() !== '';
  }
  static capitalize(text: string): string {
    if (!text) {
      return text;
    }
    return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
  }
  static capitalizeWords(text: string): string {
    return text
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .split(' ')
      .map((word) => this.capitalize(word))
      .join(' ');
  }
  static removeEnd(text: string, suffix: string) {
    if (!text || !suffix) {
      return text;
    }
    if (text.endsWith(suffix)) {
      return text.slice(0, -suffix.length);
    }
    return text;
  }
  static substringAfterLast(text: string, separator: string): string {
    if (!text || !separator) {
      return text;
    }
    const lastIndex = text.lastIndexOf(separator);
    if (lastIndex === -1) {
      return text;
    }
    return text.substring(lastIndex + separator.length);
  }
  static substringBeforeLast(text: string, separator: string) {
    if (!text || !separator) {
      return text;
    }
    const lastIndex = text.lastIndexOf(separator);
    if (lastIndex === -1) {
      return text;
    }
    return text.substring(0, lastIndex);
  }
  static replaceAll(text: string, search: string, replacement: string): string {
    if (!text || !search || !replacement) {
      return text;
    }
    return text.split(search).join(replacement);
  }

  static checkFileName(name: string): boolean {
    if (!name || name.trim() !== name) return false;
    const FILENAME_REGEX = /^[a-zA-Z0-9][\w-]*(\.[a-zA-Z0-9]+)?$/;
    return FILENAME_REGEX.test(name);
  }

  static checkFilePath(path: string): boolean {
    if (!path || path.trim() !== path) return false;
    const FILEPATH_REGEX = /^([a-zA-Z0-9][\w-]*\/)*[a-zA-Z0-9][\w-]*(\.[a-zA-Z0-9]+)?$/;
    return FILEPATH_REGEX.test(path);
  }

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
