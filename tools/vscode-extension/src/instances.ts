import * as vscode from 'vscode';
import {
  AcmClient,
  normalizeCookie,
  defaultSecret,
  enabledInstances,
  isLocalUrl,
  resolveActive,
  type AcmAuth,
  type AcmInstance,
} from '@acm/shared';
import { NAMESPACE, SETTINGS, settingId, type SettingKey } from './ids';

export type { AcmInstance };

export interface AcmTarget {
  instance: AcmInstance;
  client: AcmClient;
}

const SECRET_LABELS: Record<AcmInstance['authMode'], string> = {
  bearer: 'bearer token (IMS user token)',
  cookie: 'login-token cookie (value or name=value)',
  basic: 'password',
};

/** A memoized client with the secret and timeout it was built from, so changes rebuild it. */
interface ClientEntry {
  secret: string;
  timeout: number;
  client: AcmClient;
}

let secrets: vscode.SecretStorage;
const clients = new Map<string, ClientEntry>();

/** Reads a setting; its default comes from package.json, so no fallback is repeated in code. */
export function readSetting<T>(key: SettingKey): T {
  return vscode.workspace.getConfiguration(NAMESPACE).get<T>(key) as T;
}

/** Timings and limits from settings, in milliseconds. */
export interface AcmSettings {
  httpTimeout: number;
  runTimeout: number;
  runPollInterval: number;
  healthInterval: number;
  executionsLimit: number;
}

export function getSettings(): AcmSettings {
  return {
    httpTimeout: readSetting<number>(SETTINGS.httpTimeout),
    runTimeout: readSetting<number>(SETTINGS.runTimeout),
    runPollInterval: readSetting<number>(SETTINGS.runPollInterval),
    healthInterval: readSetting<number>(SETTINGS.healthInterval),
    executionsLimit: readSetting<number>(SETTINGS.executionsLimit),
  };
}

export function initInstances(context: vscode.ExtensionContext): void {
  secrets = context.secrets;
}

export function getInstances(): AcmInstance[] {
  return readSetting<AcmInstance[]>(SETTINGS.instances);
}

export function getEnabledInstances(): AcmInstance[] {
  return enabledInstances(getInstances());
}

export function getActiveInstance(): AcmInstance | undefined {
  return resolveActive(getInstances(), readSetting<string>(SETTINGS.activeInstance) || undefined);
}

export async function setActiveInstance(name: string): Promise<void> {
  const target = vscode.workspace.workspaceFolders
    ? vscode.ConfigurationTarget.Workspace
    : vscode.ConfigurationTarget.Global;
  await vscode.workspace.getConfiguration(NAMESPACE).update(SETTINGS.activeInstance, name, target);
}

/** Flips `enabled` on the instance at the configuration level the list is defined in. */
async function setInstanceEnabled(name: string, enabled: boolean): Promise<void> {
  const config = vscode.workspace.getConfiguration(NAMESPACE);
  const inspected = config.inspect<AcmInstance[]>(SETTINGS.instances);
  const [list, target] = inspected?.workspaceFolderValue
    ? [inspected.workspaceFolderValue, vscode.ConfigurationTarget.WorkspaceFolder]
    : inspected?.workspaceValue
      ? [inspected.workspaceValue, vscode.ConfigurationTarget.Workspace]
      : [inspected?.globalValue ?? [], vscode.ConfigurationTarget.Global];
  await config.update(
    SETTINGS.instances,
    list.map((instance) => (instance.name === name ? { ...instance, enabled } : instance)),
    target,
  );
}

interface InstanceItem extends vscode.QuickPickItem {
  instance?: AcmInstance;
  configure?: boolean;
}

export function pickInstance(placeHolder = 'Select ACM instance'): Promise<AcmInstance | undefined> {
  const instances = getInstances();
  if (instances.length === 0) {
    void vscode.window
      .showWarningMessage('ACM: No instances configured.', 'Open Settings')
      .then((action) => action && vscode.commands.executeCommand('workbench.action.openSettings', settingId(SETTINGS.instances)));
    return Promise.resolve(undefined);
  }
  return new Promise((resolve) => {
    const picker = vscode.window.createQuickPick<InstanceItem>();
    const item = (instance: AcmInstance, active?: AcmInstance): InstanceItem => ({
      label: `$(server) ${instance.name}`,
      description: instance.url,
      detail: [
        `${instance.authMode[0].toUpperCase()}${instance.authMode.slice(1)} authentication`,
        instance.readonly ? 'Read-only' : undefined,
        instance.name === active?.name ? 'Active instance' : undefined,
        instance.enabled === false ? 'Disabled' : undefined,
      ]
        .filter(Boolean)
        .join(' · '),
      buttons: [
        {
          iconPath: new vscode.ThemeIcon(instance.enabled === false ? 'eye-closed' : 'eye'),
          tooltip: instance.enabled === false ? 'Enable instance' : 'Disable instance',
        },
      ],
      instance,
    });
    const buildItems = (): InstanceItem[] => {
      const all = getInstances();
      const enabled = enabledInstances(all);
      const active = resolveActive(all, readSetting<string>(SETTINGS.activeInstance) || undefined);
      return [
        ...enabled.map((instance) => item(instance, active)),
        ...all
          .filter((instance) => instance.enabled === false)
          .map((instance) => item(instance, active)),
        { label: '', kind: vscode.QuickPickItemKind.Separator },
        { label: '$(gear) Configure Instances...', detail: 'Edit configured ACM instances', configure: true },
      ];
    };
    picker.items = buildItems();
    picker.placeholder = placeHolder;
    picker.onDidTriggerItemButton(async ({ item: picked }) => {
      if (picked.instance) {
        await setInstanceEnabled(picked.instance.name, picked.instance.enabled === false);
        picker.items = buildItems();
      }
    });
    picker.onDidAccept(() => {
      const picked = picker.selectedItems[0];
      if (picked?.configure) {
        picker.hide();
        void vscode.commands.executeCommand('workbench.action.openSettings', settingId(SETTINGS.instances));
        return;
      }
      if (!picked?.instance) {
        return;
      }
      if (picked.instance.enabled === false) {
        picker.placeholder = `"${picked.instance.name}" is disabled — enable it with the eye button first.`;
        return;
      }
      picker.hide();
      resolve(picked.instance);
    });
    picker.onDidHide(() => {
      picker.dispose();
      resolve(undefined);
    });
    picker.show();
  });
}

// Keyed by URL, auth mode and user too, so a secret is never sent to a host or as a principal it was not entered for.
function secretKey(instance: AcmInstance): string {
  return `${NAMESPACE}.secret.${instance.name}@${instance.url}#${instance.authMode}:${instance.user ?? ''}`;
}

export async function setCredentials(instance: AcmInstance): Promise<boolean> {
  if (instance.authMode === 'basic' && !instance.user) {
    vscode.window.showErrorMessage(`ACM: Set "user" for instance "${instance.name}" in ${settingId(SETTINGS.instances)}.`);
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
  if (isLocalUrl(instance.url)) {
    return true;
  }
  const answer = await vscode.window.showWarningMessage(
    `${action} on "${instance.name}" (${instance.url})?`,
    { modal: true },
    'Continue',
  );
  return answer === 'Continue';
}
