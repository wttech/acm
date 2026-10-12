import type { AcmAuthMode, AcmInstance, SecretRef } from '../domain/instance';
import { asFlag, asObject, asPositiveNumber, asText } from '../util/validate';

/** Prefix shared by VS Code settings and the MCP config document: `acm.*`. */
export const CONFIG_NAMESPACE = 'acm';

/** Setting keys relative to the `acm.` prefix, identical in VS Code settings and the MCP config document. */
export const CONFIG_KEYS = {
  instances: 'instances',
  activeInstance: 'activeInstance',
  httpTimeout: 'http.timeout',
  runTimeout: 'run.timeout',
  runPollInterval: 'run.pollInterval',
} as const;

/** The full key as it appears in settings.json and in the MCP config document, e.g. `acm.instances`. */
export const configKey = (key: (typeof CONFIG_KEYS)[keyof typeof CONFIG_KEYS]): string =>
  `${CONFIG_NAMESPACE}.${key}`;

export interface AcmConfig {
  instances: AcmInstance[];
  activeInstance?: string;
  httpTimeoutMs: number;
  runTimeoutMs: number;
  runPollIntervalMs: number;
}

/** Defaults applied when the document does not set a key; they match the extension's package.json defaults. */
export const CONFIG_DEFAULTS = {
  httpTimeoutMs: 30000,
  runTimeoutMs: 120000,
  runPollIntervalMs: 1000,
} as const;

const AUTH_MODES: readonly AcmAuthMode[] = ['bearer', 'cookie', 'basic'];

/** Validates a config document (VS Code settings or the MCP file). Unknown keys are ignored. */
export function parseConfig(doc: unknown): AcmConfig {
  try {
    return readConfig(doc);
  } catch (error) {
    throw new Error(`Invalid ACM config: ${error instanceof Error ? error.message : error}.`);
  }
}

function readConfig(doc: unknown): AcmConfig {
  const root = asObject(doc, 'the document');
  const entries = root[configKey(CONFIG_KEYS.instances)];
  if (!Array.isArray(entries) || entries.length === 0) {
    throw new Error(`"${configKey(CONFIG_KEYS.instances)}" must list at least one instance`);
  }
  const instances = entries.map(readInstance);
  const duplicate = instances.find(
    (instance, i) => instances.findIndex((other) => other.name === instance.name) !== i,
  );
  if (duplicate) {
    throw new Error(`duplicate instance name "${duplicate.name}"`);
  }
  return {
    instances,
    activeInstance: asText(
      root[configKey(CONFIG_KEYS.activeInstance)],
      `"${configKey(CONFIG_KEYS.activeInstance)}"`,
      false,
    ),
    httpTimeoutMs: readMs(root, CONFIG_KEYS.httpTimeout, CONFIG_DEFAULTS.httpTimeoutMs),
    runTimeoutMs: readMs(root, CONFIG_KEYS.runTimeout, CONFIG_DEFAULTS.runTimeoutMs),
    runPollIntervalMs: readMs(root, CONFIG_KEYS.runPollInterval, CONFIG_DEFAULTS.runPollIntervalMs),
  };
}

function readInstance(entry: unknown): AcmInstance {
  const o = asObject(entry, 'each instance');
  const name = asText(o.name, 'instance "name"')!;
  const url = asText(o.url, `instance "${name}": "url"`)!.replace(/\/+$/, '');
  try {
    new URL(url);
  } catch {
    throw new Error(`instance "${name}" has an invalid "url" "${url}"`);
  }
  const authMode = o.authMode as AcmAuthMode;
  if (!AUTH_MODES.includes(authMode)) {
    throw new Error(`instance "${name}": "authMode" must be one of ${AUTH_MODES.join(', ')}`);
  }
  const user = asText(o.user, `instance "${name}": "user"`, false);
  if (authMode === 'basic' && !user) {
    throw new Error(`instance "${name}": basic auth needs "user"`);
  }
  return {
    name,
    url,
    authMode,
    user,
    readonly: asFlag(o.readonly, `instance "${name}": "readonly"`),
    enabled: asFlag(o.enabled, `instance "${name}": "enabled"`),
    secret: readSecret(o.secret, name),
  };
}

function readSecret(value: unknown, name: string): SecretRef | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (typeof value === 'string') {
    return value;
  }
  if (typeof value === 'object' && value !== null) {
    const ref = value as Record<string, unknown>;
    if (typeof ref.env === 'string' && ref.env.trim()) {
      return { env: ref.env.trim() };
    }
    if (typeof ref.file === 'string' && ref.file.trim()) {
      return { file: ref.file.trim() };
    }
  }
  throw new Error(`instance "${name}": "secret" must be a string, { "env": "NAME" } or { "file": "path" }`);
}

function readMs(
  doc: Record<string, unknown>,
  key: (typeof CONFIG_KEYS)[keyof typeof CONFIG_KEYS],
  fallback: number,
): number {
  return asPositiveNumber(doc[configKey(key)], `"${configKey(key)}"`) ?? fallback;
}
