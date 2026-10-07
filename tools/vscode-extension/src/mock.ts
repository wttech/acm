import * as vscode from 'vscode';
import { SETTINGS, settingId } from './ids';
import { readSetting } from './instances';

type MockMode = 'auto' | 'enabled' | 'disabled';

let reported = false;
const changed = new vscode.EventEmitter<void>();

export const onDidChangeMock = changed.event;

/** `auto` follows the active instance's Mock HTTP Filter, which is off until it is configured, so unknown means off. */
export function isMockEnabled(): boolean {
  const mode = readSetting<MockMode>(SETTINGS.mockMode);
  return mode === 'auto' ? reported : mode === 'enabled';
}

/** Records whether the active instance has its Mock HTTP Filter enabled. */
export function reportInstanceMock(enabled: boolean): void {
  const before = isMockEnabled();
  reported = enabled;
  if (before !== isMockEnabled()) {
    changed.fire();
  }
}

export function registerMock(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    changed,
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration(settingId(SETTINGS.mockMode))) {
        changed.fire();
      }
    }),
  );
}
