import * as vscode from 'vscode';
import {
  ACM_API,
  ACM_FEATURE,
  AcmHttpError,
  activeInstanceProblem,
  describeHealth,
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
  startedAt: Date;
}

const MAX_TOOLTIP_ISSUES = 5;

let item: vscode.StatusBarItem;
let abortItem: vscode.StatusBarItem;
let running: RunningExecution | undefined;
let health: HealthStatus | undefined;
let failure: string | undefined;
let unauthorized = false;
let checkedAt: Date | undefined;
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
  running = executionId ? { executionId, instance: instanceName ?? '', startedAt: new Date() } : undefined;
  updateStatus();
}

/** Checks that the active instance is reachable, authorized and healthy; never prompts. */
export async function refreshHealth(): Promise<void> {
  const check = ++healthCheck;
  const instance = getActiveInstance();
  let result: HealthStatus | undefined;
  let error: string | undefined;
  let noAccess = false;
  let noHistory = false;
  let mock = false;
  if (instance) {
    const client = await getClient(instance, false);
    if (!client) {
      error = 'No credentials.';
      noAccess = true;
    } else {
      try {
        const res = await client.request<State>('GET', ACM_API.state);
        result = res.data?.healthStatus;
        noHistory = res.data?.permissions?.features?.[ACM_FEATURE.consoleExecuteNoHistory] === true;
        mock = res.data?.mockStatus?.enabled === true;
      } catch (e) {
        error = e instanceof AcmHttpError ? e.message : 'Instance is not reachable.';
        noAccess = e instanceof AcmHttpError && e.httpStatus === 401;
      }
    }
  }
  if (check === healthCheck) {
    health = result;
    failure = error;
    unauthorized = noAccess;
    checkedAt = instance ? new Date() : undefined;
    vscode.commands.executeCommand('setContext', CONTEXT.canRunWithoutHistory, noHistory);
    reportInstanceMock(mock);
    updateStatus();
  }
}

const commandLink = (command: string) => `command:${command}`;
const link = (icon: string, label: string, command: string) => `[$(${icon}) ${label}](${command})`;
const time = (date: Date) => date.toLocaleTimeString();

function statusSection(tooltip: vscode.MarkdownString): void {
  const report = describeHealth(health, MAX_TOOLTIP_ISSUES);
  if (unauthorized) {
    tooltip.appendMarkdown('$(key) **Credentials required**\n\n').appendText(`${failure} Set them to connect to this instance.`);
  } else if (failure) {
    tooltip.appendMarkdown('$(error) **Not reachable**\n\n').appendText(failure);
  } else if (!report) {
    tooltip.appendMarkdown('$(circle-large-outline) **Checking...**');
  } else if (report.level === 'ok') {
    tooltip.appendMarkdown(`$(pass-filled) **${report.headline}**\n\n`).appendText(report.summary);
  } else {
    tooltip.appendMarkdown(`$(warning) **${report.headline}**`);
    for (const issue of report.issues) {
      const tag = [issue.severity, issue.category].filter(Boolean).join(' \u00b7 ');
      tooltip.appendMarkdown('\n\n- ').appendText(`${tag ? `[${tag}] ` : ''}${issue.issue}${issue.details ? ` \u2014 ${issue.details}` : ''}`);
    }
    if (report.more) {
      tooltip.appendMarkdown('\n\n').appendText(`+${report.more} more`);
    }
    tooltip.appendMarkdown('\n\n').appendText(report.summary);
  }
  if (checkedAt) {
    tooltip.appendMarkdown('\n\n*').appendText(`Checked ${time(checkedAt)}`).appendMarkdown('*');
  }
}

function updateStatus(): void {
  const instance = getActiveInstance();
  vscode.commands.executeCommand('setContext', CONTEXT.hasInstance, !!instance);
  item.backgroundColor = undefined;
  const tooltip = new vscode.MarkdownString();
  tooltip.supportThemeIcons = true;
  tooltip.isTrusted = { enabledCommands: [COMMANDS.openUi, COMMANDS.abort, COMMANDS.selectInstance, COMMANDS.setCredentials, COMMANDS.checkConnection] };
  const selection = activeInstanceProblem(getInstances(), readSetting<string>(SETTINGS.activeInstance) || undefined);
  if (running) {
    item.text = `$(sync~spin) ACM: ${running.instance}`;
    item.command = COMMANDS.selectInstance;
    abortItem.text = '$(debug-stop) Abort';
    abortItem.tooltip = `Abort execution ${running.executionId} on ${running.instance}`;
    abortItem.command = COMMANDS.abort;
    abortItem.show();
  } else {
    abortItem.hide();
  }
  if (!instance) {
    if (!running) {
      item.text = `${selection ? '$(warning)' : '$(server)'} ACM: no instance`;
      item.command = COMMANDS.selectInstance;
    }
    tooltip.appendMarkdown('**ACM**\n\n').appendText(selection ?? 'No instance selected.');
    tooltip.appendMarkdown(`\n\n${link('server', 'Select instance', commandLink(COMMANDS.selectInstance))}`);
    item.tooltip = tooltip;
    return;
  }
  const flags = [instance.readonly && 'read-only'].filter(Boolean).join(' \u00b7 ');
  const level = unauthorized ? 'key' : failure ? 'error' : health && !health.healthy ? 'warning' : 'server';
  const icon = level === 'server' ? 'server' : level;
  tooltip.appendMarkdown(`$(${icon}) **ACM \u00b7 `).appendText(instance.name).appendMarkdown('**\n\n').appendText(instance.url);
  if (flags) {
    tooltip.appendText(` \u00b7 ${flags}`);
  }
  tooltip.appendMarkdown('\n\n---\n\n');
  statusSection(tooltip);
  if (selection) {
    tooltip.appendMarkdown('\n\n').appendText(selection);
  }
  if (running) {
    tooltip
      .appendMarkdown('\n\n---\n\n$(sync~spin) **Running** `')
      .appendText(running.executionId)
      .appendMarkdown('`\n\n')
      .appendText(`Started ${time(running.startedAt)}`)
      .appendMarkdown(`\n\n${link('debug-stop', 'Abort', commandLink(COMMANDS.abort))}`);
  }
  const actions = [link('globe', 'Open ACM', openUiLink('home'))];
  actions.push(
    unauthorized
      ? link('key', 'Set credentials', commandLink(COMMANDS.setCredentials))
      : link('plug', 'Check connection', commandLink(COMMANDS.checkConnection)),
    link('arrow-swap', 'Switch instance', commandLink(COMMANDS.selectInstance)),
  );
  tooltip.appendMarkdown(`\n\n---\n\n${actions.join(' \u00b7 ')}`);
  item.tooltip = tooltip;
  if (!running) {
    item.text = `$(${level}) ACM: ${instance.name}`;
    item.command = unauthorized ? COMMANDS.setCredentials : COMMANDS.selectInstance;
  }
  if (failure && !unauthorized) {
    item.backgroundColor = new vscode.ThemeColor('statusBarItem.errorBackground');
  } else if (failure || level === 'warning' || selection) {
    item.backgroundColor = new vscode.ThemeColor('statusBarItem.warningBackground');
  }
}
