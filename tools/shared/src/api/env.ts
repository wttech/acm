/** Environment variables of the ACM MCP server; the VS Code extension sets the same ones for its bundled server. */
export const MCP_ENV = {
  /** The ACM config document (JSON) inline; takes precedence over `ACM_CONFIG_FILE`. */
  config: 'ACM_CONFIG',
  /** Path to the ACM config document. */
  configFile: 'ACM_CONFIG_FILE',
  /** Message returned on 401; `{instance}` is replaced by the instance name. */
  unauthorizedMessage: 'ACM_UNAUTHORIZED_MESSAGE',
} as const;
