export const buildUrlWithParams = (url: string, searchParams: Record<string, string | number>) => {
  const params = new URLSearchParams();

  Object.entries(searchParams).forEach(([key, value]) => {
    if (value) {
      params.append(key, value.toString());
    }
  });

  return `${url}${params.toString() ? `?${params.toString()}` : ''}`;
};
