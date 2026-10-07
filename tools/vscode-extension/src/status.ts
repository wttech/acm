import * as vscode from 'vscode';
import { ACM_API, ACM_FEATURE, AcmHttpError, describeUnhealthy, type HealthStatus } from '@acm/shared';
import { openUiLink } from './browser';
import { COMMANDS, CONTEXT, NAMESPACE } from './ids';
import { getActiveInstance, getClient, getSettings } from './instances';
import { reportInstanceMock } from './mock';

interface State {
  healthStatus?: HealthStatus;
  permissions?: { features?: Record<string, boolean> };
  mockStatus?: { enabled?: boolean };
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
    item.text = `$(sync~spin) ACM: ${instance?.name ?? ''}`;
    item.tooltip = `Running execution ${running}. Click to abort.`;
    item.command = COMMANDS.abort;
  } else if (!instance) {
    item.text = '$(server) ACM: no instance';
    item.tooltip = 'Select ACM instance';
    item.command = COMMANDS.selectInstance;
  } else {
    item.text = `${problem ? '$(warning)' : '$(server)'} ACM: ${instance.name}`;
    const tooltip = new vscode.MarkdownString();
    tooltip.isTrusted = { enabledCommands: [COMMANDS.openUi] };
    tooltip.appendText(`${instance.url}${instance.readonly ? ' (read-only)' : ''}`);
    if (problem) {
      tooltip.appendMarkdown('\n\n').appendText(problem);
    }
    tooltip.appendMarkdown(`\n\n[Open ACM](${openUiLink('home')}) \u00b7 `);
    tooltip.appendText(unauthorized ? 'Click to set credentials.' : 'Click to switch.');
    item.tooltip = tooltip;
    item.backgroundColor = problem ? new vscode.ThemeColor('statusBarItem.warningBackground') : undefined;
    item.command = unauthorized ? COMMANDS.setCredentials : COMMANDS.selectInstance;
  }
}
