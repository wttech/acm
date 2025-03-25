export class Strings {
  static capitalize(text: string): string {
    if (!text) {
      return text;
    }
    return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
  }
  static removeEnd(text: string, suffix: string) {
    if (text.endsWith(suffix)) {
      return text.slice(0, -suffix.length);
    }
    return text;
  }
}
