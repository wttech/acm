/** Pages of the ACM web UI, as routes after `/acm#`. */
export const ACM_UI = {
  home: '',
  console: '/console',
  scripts: '/scripts',
  snippets: '/snippets',
  history: '/history',
  maintenance: '/maintenance',
} as const;

export type AcmUiPage = keyof typeof ACM_UI;

export function acmUiUrl(baseUrl: string, route = ''): string {
  const base = baseUrl.replace(/\/+$/, '');
  return route ? `${base}/acm#${route}` : `${base}/acm`;
}

export function acmUiScriptRoute(scriptId: string): string {
  return `/scripts/view/${encodeURIComponent(scriptId)}`;
}

export function acmUiExecutionRoute(executionId: string): string {
  return `/executions/view/${encodeURIComponent(executionId)}`;
}
