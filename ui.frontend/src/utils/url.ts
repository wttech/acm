export const buildUrlWithParams = (url: string, searchParams: Record<string, string> | URLSearchParams) => {
  const params = new URLSearchParams(searchParams);

  return `${url}${params.toString() ? `?${params.toString()}` : ''}`;
};
