import * as vscode from 'vscode';
import { MCP_ENV } from '@acm/shared';
import { COMMANDS, MCP_PROVIDER_ID, NAMESPACE, SETTINGS } from './ids';
import { getActiveInstance, getSecret, getSettings, pickInstance, readSetting, type AcmInstance } from './instances';

const SECRET_ENV: Record<AcmInstance['authMode'], string> = {
  bearer: MCP_ENV.token,
  cookie: MCP_ENV.cookie,
  basic: MCP_ENV.password,
};

const README = 'https://github.com/wttech/acm/blob/main/tools/mcp-server/README.md';

/** Any AI tool knows where it keeps its MCP configuration, so the prompt describes what to register, not where. */
function setupPrompt(instance: AcmInstance): string {
  const env = [
    `${MCP_ENV.baseUrl}=${instance.url}`,
    `${MCP_ENV.auth}=${instance.authMode}`,
    ...(instance.user ? [`${MCP_ENV.user}=${instance.user}`] : []),
    `${MCP_ENV.readonly}=${instance.readonly ? 'true' : 'false'}`,
  ];
  return [
    "Add the ACM (AEM Content Manager) MCP server to this tool's MCP configuration.",
    '',
    '- Transport: stdio. Command: npx. Arguments: -y @wppes/acm-mcp-server (needs Node.js 22+).',
    `- Environment variables: ${env.join(', ')}.`,
    `- The secret for auth mode ${instance.authMode} goes into ${SECRET_ENV[instance.authMode]}. Never write its value into a file that may be committed and do not ask me to paste it here: reference an environment variable or this tool's secret mechanism, and tell me where to put the value.`,
    "- Find in this tool's documentation where MCP servers are configured (user or project level) and in which format.",
    `- All variables: ${README}`,
    '- When done, verify the connection by calling the acm_health tool.',
  ].join('\n');
}

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
      if (!instance || !readSetting<boolean>(SETTINGS.mcpEnabled)) {
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
            [MCP_ENV.baseUrl]: instance.url,
            [MCP_ENV.auth]: instance.authMode,
            [MCP_ENV.user]: instance.user ?? null,
            [MCP_ENV.readonly]: instance.readonly ? 'true' : 'false',
            [MCP_ENV.runTimeout]: settings.runTimeout,
            [MCP_ENV.pollInterval]: settings.runPollInterval,
            [MCP_ENV.httpTimeout]: settings.httpTimeout,
            [MCP_ENV.unauthorizedMessage]: `401 Unauthorized on ACM instance "${instance.name}": the credentials are invalid or expired. Ask the user to run "ACM: Set Credentials" in VS Code, then restart this MCP server.`,
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
    vscode.lm.registerMcpServerDefinitionProvider(MCP_PROVIDER_ID, provider),
    vscode.commands.registerCommand(COMMANDS.copyMcpSetup, async () => {
      const instance = getActiveInstance() ?? (await pickInstance());
      if (instance) {
        await vscode.env.clipboard.writeText(setupPrompt(instance));
        vscode.window.showInformationMessage(
          `ACM: MCP setup prompt for "${instance.name}" copied. Paste it into your AI agent; it contains no secrets.`,
        );
      }
    }),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration(NAMESPACE)) {
        changed.fire();
      }
    }),
    context.secrets.onDidChange(() => changed.fire()),
  );
}
