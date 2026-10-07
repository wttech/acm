/** Environment variables of the ACM MCP server; the VS Code extension sets the same ones for its bundled server. */
export const MCP_ENV = {
  baseUrl: 'AEM_BASE_URL',
  auth: 'AEM_AUTH',
  user: 'AEM_USER',
  password: 'AEM_PASSWORD',
  token: 'AEM_TOKEN',
  cookie: 'AEM_COOKIE',
  cookieFile: 'AEM_COOKIE_FILE',
  httpTimeout: 'AEM_HTTP_TIMEOUT_MS',
  unauthorizedMessage: 'AEM_UNAUTHORIZED_MESSAGE',
  readonly: 'ACM_READONLY',
  runTimeout: 'ACM_RUN_TIMEOUT_MS',
  pollInterval: 'ACM_POLL_INTERVAL_MS',
} as const;
