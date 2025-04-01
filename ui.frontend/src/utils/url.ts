export class Urls {

  static compose = (url: string, searchParams: Record<string, string> | URLSearchParams) => {
    const params = new URLSearchParams(searchParams);
    return `${url}${params.toString() ? `?${params.toString()}` : ''}`;
  };
}