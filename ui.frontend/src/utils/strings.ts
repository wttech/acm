export class Strings {
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
    const FILENAME_REGEX = /^[a-zA-Z][\w-]*$/;
    return FILENAME_REGEX.test(name);
  }

  static checkFilePath(path: string): boolean {
    if (!path || path.trim() !== path) return false;
    const FILEPATH_REGEX = /^([a-zA-Z][\w-]*\/)*[a-zA-Z][\w-]*$/;
    return FILEPATH_REGEX.test(path);
  }
}
