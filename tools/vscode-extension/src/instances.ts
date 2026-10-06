import * as vscode from 'vscode';
import { AcmClient, normalizeCookie, type AcmAuth } from '@acm/shared';

export interface AcmInstance {
  name: string;
  url: string;
  authMode: 'bearer' | 'cookie' | 'basic';
  user?: string;
  readonly?: boolean;
}

export interface AcmTarget {
  instance: AcmInstance;
  client: AcmClient;
}

const SECRET_LABELS: Record<AcmInstance['authMode'], string> = {
  bearer: 'bearer token (IMS user token)',
  cookie: 'login-token cookie (value or name=value)',
  basic: 'password',
};

let secrets: vscode.SecretStorage;
const clients = new Map<string, { secret: string; timeout: number; client: AcmClient }>();

/** Timings and limits from settings, in milliseconds; defaults are declared in package.json. */
export interface AcmSettings {
  httpTimeout: number;
  runTimeout: number;
  runPollInterval: number;
  healthInterval: number;
  executionsLimit: number;
}

export function getSettings(): AcmSettings {
  const config = vscode.workspace.getConfiguration('acm');
  return {
    httpTimeout: config.get<number>('http.timeout', 30000),
    runTimeout: config.get<number>('run.timeout', 120000),
    runPollInterval: config.get<number>('run.pollInterval', 1000),
    healthInterval: config.get<number>('health.interval', 60000),
    executionsLimit: config.get<number>('executions.limit', 50),
  };
}

export function initInstances(context: vscode.ExtensionContext): void {
  secrets = context.secrets;
}

export function getInstances(): AcmInstance[] {
  return vscode.workspace.getConfiguration('acm').get<AcmInstance[]>('instances', []);
}

export function getActiveInstance(): AcmInstance | undefined {
  const instances = getInstances();
  const name = vscode.workspace.getConfiguration('acm').get<string>('activeInstance');
  return instances.find((instance) => instance.name === name) ?? (instances.length === 1 ? instances[0] : undefined);
}

export async function setActiveInstance(name: string): Promise<void> {
  const target = vscode.workspace.workspaceFolders
    ? vscode.ConfigurationTarget.Workspace
    : vscode.ConfigurationTarget.Global;
  await vscode.workspace.getConfiguration('acm').update('activeInstance', name, target);
}

export async function pickInstance(placeHolder = 'Select ACM instance'): Promise<AcmInstance | undefined> {
  const instances = getInstances();
  if (instances.length === 0) {
    const action = await vscode.window.showWarningMessage('ACM: No instances configured.', 'Open Settings');
    if (action) {
      await vscode.commands.executeCommand('workbench.action.openSettings', 'acm.instances');
    }
    return undefined;
  }
  const active = getActiveInstance();
  const picked = await vscode.window.showQuickPick(
    instances.map((instance) => ({
      label: instance.name,
      description: instance.url,
      detail: [instance.authMode, instance.readonly ? 'read-only' : undefined, instance === active ? 'active' : undefined]
        .filter(Boolean)
        .join(' · '),
      instance,
    })),
    { placeHolder },
  );
  return picked?.instance;
}

// Keyed by URL, auth mode and user too, so a secret is never sent to a host or as a principal it was not entered for.
function secretKey(instance: AcmInstance): string {
  return `acm.secret.${instance.name}@${instance.url}#${instance.authMode}:${instance.user ?? ''}`;
}

export async function setCredentials(instance: AcmInstance): Promise<boolean> {
  if (instance.authMode === 'basic' && !instance.user) {
    vscode.window.showErrorMessage(`ACM: Set "user" for instance "${instance.name}" in acm.instances.`);
    return false;
  }
  const value = await vscode.window.showInputBox({
    title: `ACM: Credentials for ${instance.name}`,
    prompt: `Enter the ${SECRET_LABELS[instance.authMode]}${instance.user ? ` of ${instance.user}` : ''} for ${instance.url}`,
    password: true,
    ignoreFocusOut: true,
  });
  if (!value) {
    return false;
  }
  await secrets.store(secretKey(instance), value.trim());
  return true;
}

/** The token, cookie or password of the instance; with `interactive`, asks for it when missing. */
export async function getSecret(instance: AcmInstance, interactive = true): Promise<string | undefined> {
  const secret = (await secrets.get(secretKey(instance))) ?? defaultSecret(instance);
  if (!secret && interactive && (await setCredentials(instance))) {
    return secrets.get(secretKey(instance));
  }
  return secret;
}

/** Builds a client for the instance; with `interactive`, asks for missing credentials. */
export async function getClient(instance: AcmInstance, interactive = true): Promise<AcmClient | undefined> {
  const secret = await getSecret(instance, interactive);
  if (!secret) {
    return undefined;
  }
  const key = secretKey(instance);
  const timeout = getSettings().httpTimeout;
  const cached = clients.get(key);
  if (cached?.secret === secret && cached.timeout === timeout) {
    return cached.client;
  }
  const client = new AcmClient({
    baseUrl: instance.url,
    auth: toAuth(instance, secret),
    timeoutMs: timeout,
    messages: {
      unauthorized: `401 Unauthorized on "${instance.name}": the ${SECRET_LABELS[instance.authMode]} is invalid or expired. Run "ACM: Set Credentials".`,
    },
  });
  clients.set(key, { secret, timeout, client });
  return client;
}

function toAuth(instance: AcmInstance, secret: string): AcmAuth {
  switch (instance.authMode) {
    case 'bearer':
      return { mode: 'bearer', token: secret };
    case 'cookie':
      return { mode: 'cookie', cookie: () => normalizeCookie(secret) };
    case 'basic':
      return { mode: 'basic', user: instance.user ?? '', password: secret };
  }
}

/** Active instance (asking to pick one if unset) with a client. */
export async function getTarget(): Promise<AcmTarget | undefined> {
  let instance = getActiveInstance();
  if (!instance) {
    instance = await pickInstance();
    if (!instance) {
      return undefined;
    }
    await setActiveInstance(instance.name);
  }
  const client = await getClient(instance);
  return client ? { instance, client } : undefined;
}

/** Blocks read-only instances and asks before running on non-local ones. */
export async function confirmRun(instance: AcmInstance, action: string): Promise<boolean> {
  if (instance.readonly) {
    vscode.window.showErrorMessage(`ACM: Instance "${instance.name}" is read-only, ${action.toLowerCase()} is blocked.`);
    return false;
  }
  if (isLocal(instance.url)) {
    return true;
  }
  const answer = await vscode.window.showWarningMessage(
    `${action} on "${instance.name}" (${instance.url})?`,
    { modal: true },
    'Continue',
  );
  return answer === 'Continue';
}

function isLocal(url: string): boolean {
  try {
    return ['localhost', '127.0.0.1', '[::1]'].includes(new URL(url).hostname);
  } catch {
    return false;
  }
}

// AEM SDK out of the box: admin/admin, only ever for local instances.
function defaultSecret(instance: AcmInstance): string | undefined {
  return instance.authMode === 'basic' && instance.user === 'admin' && isLocal(instance.url) ? 'admin' : undefined;
}
