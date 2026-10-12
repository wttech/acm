import * as vscode from 'vscode';
import {
  ACM_API,
  ACM_FEATURE,
  AcmHttpError,
  activeInstanceProblem,
  describeUnhealthy,
  type HealthStatus,
} from '@acm/shared';
import { openUiLink } from './browser';
import { COMMANDS, CONTEXT, NAMESPACE, SETTINGS } from './ids';
import { getActiveInstance, getClient, getInstances, getSettings, readSetting } from './instances';
import { reportInstanceMock } from './mock';

interface StatePermissions {
  features?: Record<string, boolean>;
}

interface MockStatus {
  enabled?: boolean;
}

interface State {
  healthStatus?: HealthStatus;
  permissions?: StatePermissions;
  mockStatus?: MockStatus;
}

/** The execution currently running, shown in the status bar with its own abort item. */
interface RunningExecution {
  executionId: string;
  instance: string;
}

let item: vscode.StatusBarItem;
let abortItem: vscode.StatusBarItem;
let running: RunningExecution | undefined;
let problem: string | undefined;
let unauthorized = false;
let healthCheck = 0;
let healthTimer: ReturnType<typeof setInterval> | undefined;

export function registerStatus(context: vscode.ExtensionContext): void {
  item = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 0);
  abortItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 0);
  context.subscriptions.push(
    item,
    abortItem,
    { dispose: () => clearInterval(healthTimer) },
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration(NAMESPACE)) {
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

export function setRunning(executionId: string | undefined, instanceName?: string): void {
  running = executionId ? { executionId, instance: instanceName ?? '' } : undefined;
  updateStatus();
}

/** Checks that the active instance is reachable, authorized and healthy; never prompts. */
export async function refreshHealth(): Promise<void> {
  const check = ++healthCheck;
  const instance = getActiveInstance();
  let result: string | undefined;
  let noAccess = false;
  let noHistory = false;
  let mock = false;
  if (instance) {
    const client = await getClient(instance, false);
    if (!client) {
      result = 'No credentials.';
      noAccess = true;
    } else {
      try {
        const res = await client.request<State>('GET', ACM_API.state);
        const health = res.data?.healthStatus;
        noHistory = res.data?.permissions?.features?.[ACM_FEATURE.consoleExecuteNoHistory] === true;
        mock = res.data?.mockStatus?.enabled === true;
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
    vscode.commands.executeCommand('setContext', CONTEXT.canRunWithoutHistory, noHistory);
    reportInstanceMock(mock);
    updateStatus();
  }
}

function updateStatus(): void {
  const instance = getActiveInstance();
  vscode.commands.executeCommand('setContext', CONTEXT.hasInstance, !!instance);
  item.backgroundColor = undefined;
  if (running) {
    item.text = `$(sync~spin) ACM: ${running.instance}`;
    item.tooltip = `Running execution ${running.executionId} on ${running.instance}. Click to switch instance.`;
    item.command = COMMANDS.selectInstance;
    abortItem.text = '$(debug-stop) Abort';
    abortItem.tooltip = `Abort execution ${running.executionId} on ${running.instance}`;
    abortItem.command = COMMANDS.abort;
    abortItem.show();
    return;
  }
  abortItem.hide();
  const selection = activeInstanceProblem(getInstances(), readSetting<string>(SETTINGS.activeInstance) || undefined);
  if (!instance) {
    item.text = `${selection ? '$(warning)' : '$(server)'} ACM: no instance`;
    item.tooltip = `${selection ? `${selection}\n\n` : ''}Select ACM instance`;
    item.command = COMMANDS.selectInstance;
  } else {
    item.text = `${problem || selection ? '$(warning)' : '$(server)'} ACM: ${instance.name}`;
    const tooltip = new vscode.MarkdownString();
    tooltip.isTrusted = { enabledCommands: [COMMANDS.openUi] };
    tooltip.appendText(`${instance.url}${instance.readonly ? ' (read-only)' : ''}`);
    for (const warning of [selection, problem]) {
      if (warning) {
        tooltip.appendMarkdown('\n\n').appendText(warning);
      }
    }
    tooltip.appendMarkdown(`\n\n[Open ACM](${openUiLink('home')}) \u00b7 `);
    tooltip.appendText(unauthorized ? 'Click to set credentials.' : 'Click to switch.');
    item.tooltip = tooltip;
    item.backgroundColor = problem ? new vscode.ThemeColor('statusBarItem.warningBackground') : undefined;
    item.command = unauthorized ? COMMANDS.setCredentials : COMMANDS.selectInstance;
  }
}
