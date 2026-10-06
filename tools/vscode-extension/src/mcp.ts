import * as vscode from 'vscode';
import { getActiveInstance, getSecret, getSettings, type AcmInstance } from './instances';

const SECRET_ENV: Record<AcmInstance['authMode'], string> = {
  bearer: 'AEM_TOKEN',
  cookie: 'AEM_COOKIE',
  basic: 'AEM_PASSWORD',
};

/** Registers the bundled ACM MCP server for the active instance, so agents get ACM tools without setup. */
export function registerMcp(context: vscode.ExtensionContext): void {
  const changed = new vscode.EventEmitter<void>();
  const servers = new Map<string, AcmInstance>();
  const version = (context.extension.packageJSON as { version: string }).version;

  const provider: vscode.McpServerDefinitionProvider<vscode.McpStdioServerDefinition> = {
    onDidChangeMcpServerDefinitions: changed.event,
    provideMcpServerDefinitions() {
      servers.clear();
      const instance = getActiveInstance();
      if (!instance || !vscode.workspace.getConfiguration('acm').get<boolean>('mcp.enabled', true)) {
        return [];
      }
      const label = `ACM (${instance.name})`;
      const settings = getSettings();
      servers.set(label, instance);
      // The editor's own Node.js runs the server, so users need no Node.js install.
      return [
        new vscode.McpStdioServerDefinition(
          label,
          process.execPath,
          [context.asAbsolutePath('dist/mcp-server.mjs')],
          {
            AEM_BASE_URL: instance.url,
            AEM_AUTH: instance.authMode,
            AEM_USER: instance.user ?? null,
            ACM_READONLY: instance.readonly ? 'true' : 'false',
            ACM_RUN_TIMEOUT_MS: settings.runTimeout,
            ACM_POLL_INTERVAL_MS: settings.runPollInterval,
            AEM_HTTP_TIMEOUT_MS: settings.httpTimeout,
            AEM_UNAUTHORIZED_MESSAGE: `401 Unauthorized on ACM instance "${instance.name}": the credentials are invalid or expired. Ask the user to run "ACM: Set Credentials" in VS Code, then restart this MCP server.`,
          },
          `${version}+${instance.name}`,
        ),
      ];
    },
    // Secrets are added only when the server starts, never listed with the definition.
    async resolveMcpServerDefinition(server) {
      const instance = servers.get(server.label);
      const secret = instance && (await getSecret(instance));
      if (!instance || !secret) {
        return undefined;
      }
      server.env = { ...server.env, [SECRET_ENV[instance.authMode]]: secret };
      return server;
    },
  };

  context.subscriptions.push(
    changed,
    vscode.lm.registerMcpServerDefinitionProvider('acm', provider),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration('acm')) {
        changed.fire();
      }
    }),
    context.secrets.onDidChange(() => changed.fire()),
  );
}
