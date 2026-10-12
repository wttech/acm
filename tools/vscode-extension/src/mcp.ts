import * as vscode from 'vscode';
import { CONFIG_KEYS, MCP_ENV, configKey, type AcmInstance } from '@acm/shared';
import { COMMANDS, MCP_PROVIDER_ID, NAMESPACE, SETTINGS } from './ids';
import {
  getActiveInstance,
  getEnabledInstances,
  getInstances,
  getSecret,
  getSettings,
  pickInstance,
  readSetting,
} from './instances';

const README = 'https://github.com/wttech/acm/blob/main/tools/mcp-server/README.md';

/** One environment variable per instance; names that sanitize alike get the index, so a secret never reaches another instance. */
function secretEnvs(instances: AcmInstance[]): Map<string, string> {
  const envs = new Map<string, string>();
  const taken = new Set<string>();
  instances.forEach((instance, index) => {
    const base = `ACM_SECRET_${instance.name.toUpperCase().replace(/[^A-Z0-9]/g, '_')}`;
    const env = taken.has(base) ? `${base}_${index}` : base;
    taken.add(env);
    envs.set(instance.name, env);
  });
  return envs;
}

const instanceEntry = (instance: AcmInstance, secretEnv?: string) => ({
  name: instance.name,
  url: instance.url,
  authMode: instance.authMode,
  ...(instance.user ? { user: instance.user } : {}),
  ...(instance.readonly ? { readonly: true } : {}),
  ...(instance.enabled === false ? { enabled: false } : {}),
  ...(secretEnv ? { secret: { env: secretEnv } } : {}),
});

/** The config document the bundled MCP server reads; secrets are env references resolved at server start. */
function configDoc(instances: AcmInstance[], secrets: Map<string, string>): Record<string, unknown> {
  const settings = getSettings();
  const envs = secretEnvs(instances);
  const active = getActiveInstance()?.name;
  return {
    [configKey(CONFIG_KEYS.instances)]: instances.map((instance) =>
      instanceEntry(instance, secrets.has(instance.name) ? envs.get(instance.name) : undefined),
    ),
    ...(active ? { [configKey(CONFIG_KEYS.activeInstance)]: active } : {}),
    [configKey(CONFIG_KEYS.httpTimeout)]: settings.httpTimeout,
    [configKey(CONFIG_KEYS.runTimeout)]: settings.runTimeout,
    [configKey(CONFIG_KEYS.runPollInterval)]: settings.runPollInterval,
  };
}

/** The doc the user exports: every configured instance, with env refs as placeholders for the secrets to fill in. */
function exportableConfig(): Record<string, unknown> {
  const all = getInstances();
  return configDoc(
    all,
    new Map(all.map((instance) => [instance.name, ''])),
  );
}

/** Any AI tool knows where it keeps its MCP configuration, so the prompt describes what to register, not where. */
function setupPrompt(): string {
  const vars = [...secretEnvs(getInstances()).values()].join(', ');
  return [
    "Add the ACM (AEM Content Manager) MCP server to this tool's MCP configuration.",
    '',
    '- Transport: stdio. Command: npx. Arguments: -y @wppes/acm-mcp-server (needs Node.js 22+).',
    `- Environment variable: ${MCP_ENV.configFile}, pointing at a JSON file with this content:`,
    '',
    JSON.stringify(exportableConfig(), null, 2),
    '',
    `- Each "secret" references an environment variable (${vars}). Put the values in this tool's secret mechanism or export them before starting the server; never write them into the file or ask me to paste them here.`,
    "- Find in this tool's documentation where MCP servers are configured (user or project level) and in which format.",
    `- Config format: ${README}`,
    `- When done, verify the connection by calling the acm_list_instances and acm_health tools.`,
  ].join('\n');
}

const hash = (s: string) => {
  let h = 5381;
  for (const c of s) h = (h * 33) ^ c.charCodeAt(0);
  return (h >>> 0).toString(36);
};

/** Registers the bundled ACM MCP server for all enabled instances, so agents get ACM tools without setup. */
export function registerMcp(context: vscode.ExtensionContext): void {
  const changed = new vscode.EventEmitter<void>();
  const servers = new Map<string, AcmInstance[]>();
  const version = (context.extension.packageJSON as { version: string }).version;

  const provider: vscode.McpServerDefinitionProvider<vscode.McpStdioServerDefinition> = {
    onDidChangeMcpServerDefinitions: changed.event,
    provideMcpServerDefinitions() {
      servers.clear();
      const instances = getEnabledInstances();
      if (instances.length === 0 || !readSetting<boolean>(SETTINGS.mcpEnabled)) {
        return [];
      }
      const label = 'ACM';
      servers.set(label, instances);
      const doc = JSON.stringify(configDoc(instances, new Map()));
      // The editor's own Node.js runs the server, so users need no Node.js install.
      return [
        new vscode.McpStdioServerDefinition(
          label,
          process.execPath,
          [context.asAbsolutePath('dist/mcp-server.mjs')],
          {
            [MCP_ENV.config]: doc,
            [MCP_ENV.unauthorizedMessage]: `401 Unauthorized on ACM instance "{instance}": the credentials are invalid or expired. Ask the user to run "ACM: Set Credentials" in VS Code, then restart this MCP server.`,
          },
          `${version}+${hash(doc)}`,
        ),
      ];
    },
    // Secrets are added only when the server starts, never listed with the definition.
    async resolveMcpServerDefinition(server) {
      const instances = servers.get(server.label);
      if (!instances) {
        return undefined;
      }
      const secrets = new Map<string, string>();
      for (const instance of instances) {
        const secret = await getSecret(instance, false);
        if (secret) {
          secrets.set(instance.name, secret);
        }
      }
      const envs = secretEnvs(instances);
      server.env = {
        ...server.env,
        [MCP_ENV.config]: JSON.stringify(configDoc(instances, secrets)),
        ...Object.fromEntries([...secrets].map(([name, secret]) => [envs.get(name)!, secret])),
      };
      return server;
    },
  };

  context.subscriptions.push(
    changed,
    vscode.lm.registerMcpServerDefinitionProvider(MCP_PROVIDER_ID, provider),
    vscode.commands.registerCommand(COMMANDS.copyMcpSetup, async () => {
      const instances = getEnabledInstances();
      const list = instances.length > 0 ? instances : [await pickInstance()].filter((i): i is AcmInstance => !!i);
      if (list.length > 0) {
        await vscode.env.clipboard.writeText(setupPrompt());
        vscode.window.showInformationMessage(
          `ACM: MCP setup prompt for ${list.map((instance) => `"${instance.name}"`).join(', ')} copied. Paste it into your AI agent; it contains no secrets.`,
        );
      }
    }),
    vscode.commands.registerCommand(COMMANDS.exportMcpConfig, async () => {
      const instances = getInstances();
      if (instances.length === 0) {
        vscode.window.showWarningMessage('ACM: No instances configured.');
        return;
      }
      const target = await vscode.window.showSaveDialog({ filters: { 'ACM config': ['json'] } });
      if (!target) {
        return;
      }
      const doc = exportableConfig();
      await vscode.workspace.fs.writeFile(target, new TextEncoder().encode(JSON.stringify(doc, null, 2) + '\n'));
      const action = await vscode.window.showInformationMessage(
        `ACM: Exported ${instances.length} instance(s) without secrets. ` +
          `Fill in "secret" (literal, { "env": "NAME" } or { "file": "path" }) and point ${MCP_ENV.configFile} at this file.`,
        'Open',
      );
      if (action) {
        await vscode.commands.executeCommand('vscode.open', target);
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
