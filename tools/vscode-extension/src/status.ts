import * as vscode from 'vscode';
import { ACM_API, AcmHttpError, describeUnhealthy, type HealthStatus } from '@acm/shared';
import { getActiveInstance, getClient, getSettings } from './instances';

interface State {
  healthStatus?: HealthStatus;
  permissions?: { features?: Record<string, boolean> };
}

let item: vscode.StatusBarItem;
let running: string | undefined;
let problem: string | undefined;
let unauthorized = false;
let healthCheck = 0;
let healthTimer: ReturnType<typeof setInterval> | undefined;

export function registerStatus(context: vscode.ExtensionContext): void {
  item = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 0);
  context.subscriptions.push(
    item,
    { dispose: () => clearInterval(healthTimer) },
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration('acm')) {
        scheduleHealth();
        void refreshHealth();
      }
    }),
    context.secrets.onDidChange(() => void refreshHealth()),
  );
  updateStatus();
  item.show();
  scheduleHealth();
  void refreshHealth();
}

function scheduleHealth(): void {
  clearInterval(healthTimer);
  const interval = getSettings().healthInterval;
  healthTimer = interval > 0 ? setInterval(() => void refreshHealth(), interval) : undefined;
}

export function setRunning(executionId: string | undefined): void {
  running = executionId;
  updateStatus();
}

/** Checks that the active instance is reachable, authorized and healthy; never prompts. */
export async function refreshHealth(): Promise<void> {
  const check = ++healthCheck;
  const instance = getActiveInstance();
  let result: string | undefined;
  let noAccess = false;
  let noHistory = false;
  if (instance) {
    const client = await getClient(instance, false);
    if (!client) {
      result = 'No credentials.';
      noAccess = true;
    } else {
      try {
        const res = await client.request<State>('GET', ACM_API.state);
        const health = res.data?.healthStatus;
        noHistory = res.data?.permissions?.features?.['console.execute.nohistory'] === true;
        result = describeUnhealthy(health);
      } catch (e) {
        result = e instanceof AcmHttpError ? e.message : 'Instance is not reachable.';
        noAccess = e instanceof AcmHttpError && e.httpStatus === 401;
      }
    }
  }
  if (check === healthCheck) {
    problem = result;
    unauthorized = noAccess;
    vscode.commands.executeCommand('setContext', 'acm.canRunWithoutHistory', noHistory);
    updateStatus();
  }
}

function updateStatus(): void {
  const instance = getActiveInstance();
  vscode.commands.executeCommand('setContext', 'acm.hasInstance', !!instance);
  item.backgroundColor = undefined;
  if (running) {
    item.text = `$(sync~spin) ACM: ${instance?.name ?? ''}`;
    item.tooltip = `Running execution ${running}. Click to abort.`;
    item.command = 'acm.abort';
  } else if (!instance) {
    item.text = '$(server) ACM: no instance';
    item.tooltip = 'Select ACM instance';
    item.command = 'acm.selectInstance';
  } else {
    item.text = `${problem ? '$(warning)' : '$(server)'} ACM: ${instance.name}`;
    item.tooltip = [
      `${instance.url}${instance.readonly ? ' (read-only)' : ''}`,
      problem,
      unauthorized ? 'Click to set credentials.' : 'Click to switch.',
    ]
      .filter(Boolean)
      .join('\n');
    item.backgroundColor = problem ? new vscode.ThemeColor('statusBarItem.warningBackground') : undefined;
    item.command = unauthorized ? 'acm.setCredentials' : 'acm.selectInstance';
  }
}
