import * as vscode from 'vscode';
import type { AcmConnection } from '@acm/shared';

export interface AcmInstance {
  name: string;
  url: string;
  authMode: 'bearer' | 'cookie' | 'basic';
  user?: string;
  readonly?: boolean;
}

export function getInstances(): AcmInstance[] {
  return vscode.workspace.getConfiguration('acm').get<AcmInstance[]>('instances', []);
}

export function getActiveInstance(): AcmInstance | undefined {
  const name = vscode.workspace.getConfiguration('acm').get<string>('activeInstance');
  return getInstances().find((instance) => instance.name === name);
}

export async function setActiveInstance(name: string): Promise<void> {
  await vscode.workspace.getConfiguration('acm').update('activeInstance', name, vscode.ConfigurationTarget.Workspace);
}

// TODO: read the token/cookie/password from context.secrets (never from settings) and build the connection.
export async function getConnection(
  _context: vscode.ExtensionContext,
  _instance: AcmInstance,
): Promise<AcmConnection | undefined> {
  return undefined;
}
