export class Objects {
  static isNotEmpty(obj?: object): boolean {
    return !!obj && Object.keys(obj).length > 0;
  }

  static isEmpty(obj?: object): boolean {
    return !obj || Object.keys(obj).length === 0;
  }
}
