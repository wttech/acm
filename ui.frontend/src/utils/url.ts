export class Urls {
  static asset = (assetName: string) => `${import.meta.env.BASE_URL}${assetName}`;
  static compose = (url: string, searchParams: Record<string, string> | URLSearchParams) => {
    const params = new URLSearchParams(searchParams);
    return `${url}${params.toString() ? `?${params.toString()}` : ''}`;
  };
}
