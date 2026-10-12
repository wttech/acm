export type AcmAuthMode = 'bearer' | 'cookie' | 'basic';

/** Where the MCP server gets an instance's secret; the VS Code extension keeps secrets in secret storage instead. */
export type SecretRef = string | { env: string } | { file: string };

export interface AcmInstance {
  name: string;
  url: string;
  authMode: AcmAuthMode;
  user?: string;
  /** Block the tools that run or abort code on this instance. */
  readonly?: boolean;
  /** Disabled instances are hidden from pickers, views and the MCP server (default true). */
  enabled?: boolean;
  /** MCP config document only. */
  secret?: SecretRef;
}

export function enabledInstances(instances: AcmInstance[]): AcmInstance[] {
  return instances.filter((instance) => instance.enabled !== false);
}

/** The active enabled instance, else the only enabled one; `undefined` when the user must pick. */
export function resolveActive(instances: AcmInstance[], activeName?: string): AcmInstance | undefined {
  const enabled = enabledInstances(instances);
  return enabled.find((instance) => instance.name === activeName) ?? (enabled.length === 1 ? enabled[0] : undefined);
}

const enabledNames = (instances: AcmInstance[]) =>
  enabledInstances(instances)
    .map((instance) => instance.name)
    .join(', ');

/** The instance a tool call targets: the requested one, the active one, or the only enabled one. */
export function resolveInstance(
  instances: AcmInstance[],
  activeName: string | undefined,
  requested?: string,
): AcmInstance {
  const enabled = enabledInstances(instances);
  if (enabled.length === 0) {
    throw new Error('No enabled ACM instances: every instance in the config is disabled.');
  }
  if (requested) {
    const found = enabled.find((instance) => instance.name === requested);
    if (!found) {
      throw new Error(
        `ACM instance "${requested}" is not configured or is disabled. Enabled instances: ${enabledNames(instances)}.`,
      );
    }
    return found;
  }
  const active = resolveActive(instances, activeName);
  if (active) {
    return active;
  }
  throw new Error(`No active ACM instance. Pass "instance" with one of: ${enabledNames(instances)}.`);
}

export function isLocalUrl(url: string): boolean {
  try {
    return ['localhost', '127.0.0.1', '[::1]'].includes(new URL(url).hostname);
  } catch {
    return false;
  }
}

// AEM SDK out of the box: admin/admin, only ever for local instances.
export function defaultSecret(instance: AcmInstance): string | undefined {
  return instance.authMode === 'basic' && instance.user === 'admin' && isLocalUrl(instance.url) ? 'admin' : undefined;
}
