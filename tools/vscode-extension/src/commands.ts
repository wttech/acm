import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  ACM_API,
  CONSOLE_CODE_ID,
  fetchConsoleOutput,
  fetchExecutionById,
  isFailed,
  isPending,
  normalizeGroovy,
  SCRIPT_TEMPLATES,
  type Execution,
  type QueueOutput,
} from '@acm/shared';
import { promptInputs } from './inputs';
import {
  confirmRun,
  getActiveInstance,
  getClient,
  getTarget,
  pickInstance,
  setActiveInstance,
  setCredentials,
  type AcmInstance,
  type AcmTarget,
} from './instances';
import { validateDocument } from './providers/diagnostics';
import { setRunning } from './status';
import { executionUri, type Views } from './views';

const POLL_INTERVAL_MS = 1000;

let output: vscode.OutputChannel;
let running: (AcmTarget & { executionId: string }) | undefined;

export function registerCommands(context: vscode.ExtensionContext, views: Views): void {
  output = vscode.window.createOutputChannel('ACM');
  const register = (command: string, handler: (...args: never[]) => Promise<unknown>) =>
    vscode.commands.registerCommand(command, (...args: unknown[]) => handler(...(args as never[])).catch(showError));
  context.subscriptions.push(
    output,
    register('acm.run', () => run(false, views)),
    register('acm.runSelection', () => run(true, views)),
    register('acm.validate', validate),
    register('acm.describe', describe),
    register('acm.abort', (node?: { instance: AcmInstance; execution: Execution }) => abort(views, node)),
    register('acm.selectInstance', selectInstance),
    register('acm.setCredentials', setCredentialsCommand),
    register('acm.checkConnection', checkConnection),
    register('acm.newScript', newScript),
  );
}

async function newScript(): Promise<void> {
  const picked = await vscode.window.showQuickPick(
    SCRIPT_TEMPLATES.map((template) => ({ label: template.name, detail: template.description, template })),
    { placeHolder: 'Select ACM script template', matchOnDetail: true },
  );
  if (picked) {
    const document = await vscode.workspace.openTextDocument({ language: 'groovy', content: picked.template.code });
    await vscode.window.showTextDocument(document);
  }
}

async function run(selectionOnly: boolean, views: Views): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  if (!editor) {
    vscode.window.showWarningMessage('ACM: Open a Groovy script to run.');
    return;
  }
  if (running) {
    vscode.window.showWarningMessage(`ACM: Execution ${running.executionId} is still running.`);
    return;
  }
  const target = await getTarget();
  if (!target || !(await confirmRun(target.instance, 'Run script'))) {
    return;
  }
  const { instance, client } = target;
  const selection = selectionOnly && !editor.selection.isEmpty ? editor.selection : undefined;
  const content = normalizeGroovy(editor.document.getText(selection));
  const inputs = /\bdescribeRun\s*\(/.test(content) ? await promptInputs(client, content) : {};
  if (!inputs) {
    return;
  }
  const label = path.basename(editor.document.fileName);

  output.show(true);
  output.appendLine(`Running ${label}${selection ? ' (selection)' : ''} on ${instance.name} (${instance.url})`);
  const res = await client.request<QueueOutput>('POST', ACM_API.queueCode, {
    code: { id: CONSOLE_CODE_ID, content },
    inputs: Object.keys(inputs).length > 0 ? inputs : undefined,
  });
  let execution = res.data?.executions?.[0];
  if (!execution) {
    throw new Error(`Queued, but no execution returned: ${res.message}`);
  }
  const executionId = execution.id;

  running = { ...target, executionId };
  setRunning(executionId);
  views.refreshExecutions();
  try {
    let printed = 0;
    await vscode.window.withProgress(
      { location: vscode.ProgressLocation.Notification, title: `ACM: Script ${label} on ${instance.name}`, cancellable: true },
      async (progress, token) => {
        token.onCancellationRequested(() => abort(views).catch(showError));
        while (isPending(execution!.status)) {
          progress.report({ message: execution!.status.toLowerCase() });
          await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
          execution = (await fetchExecutionById(client, executionId)) ?? execution;
          printed = append(execution!.output, printed);
        }
      },
    );
    append((await fetchConsoleOutput(client, executionId)) ?? execution.output, printed);
    report(label, execution, instance);
  } finally {
    running = undefined;
    setRunning(undefined);
    views.refreshExecutions();
  }
}

function append(text: string | null | undefined, printed: number): number {
  if (!text || text.length <= printed) {
    return printed;
  }
  output.append(text.slice(printed));
  return text.length;
}

function report(label: string, execution: Execution, instance: AcmInstance): void {
  if (execution.error) {
    output.appendLine(`\n${execution.error}`);
  }
  const status = execution.status.toUpperCase();
  output.appendLine(
    `\n${status}${execution.duration !== undefined ? ` in ${execution.duration} ms` : ''} (execution ${execution.id})\n`,
  );
  const open = () => vscode.commands.executeCommand('vscode.open', executionUri(instance, execution.id));
  if (isFailed(status)) {
    vscode.window.showErrorMessage(`ACM: ${label} ${status}.`, 'Show Output', 'Open Execution').then((action) => {
      if (action === 'Show Output') output.show();
      if (action === 'Open Execution') open();
    });
  } else if (status === 'SKIPPED') {
    vscode.window.showInformationMessage(`ACM: ${label} SKIPPED, canRun() returned false.`);
  } else {
    vscode.window.setStatusBarMessage(`$(pass) ACM: ${label} ${status}`, 5000);
  }
}

async function validate(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  const target = editor && (await getTarget());
  if (!editor || !target) {
    return;
  }
  const error = await validateDocument(editor.document, target.client);
  if (error) {
    vscode.window.showErrorMessage('ACM: Script does not compile, see Problems.');
  } else {
    vscode.window.showInformationMessage(`ACM: Script compiles on ${target.instance.name}.`);
  }
}

async function describe(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  const target = editor && (await getTarget());
  // ACM runs describeRun() to resolve inputs, so it is guarded like a run.
  if (!editor || !target || !(await confirmRun(target.instance, 'Describe inputs'))) {
    return;
  }
  const res = await target.client.request<{ inputs?: unknown }>('POST', ACM_API.describeCode, {
    code: { id: CONSOLE_CODE_ID, content: normalizeGroovy(editor.document.getText()) },
  });
  output.appendLine(`Inputs of ${path.basename(editor.document.fileName)} on ${target.instance.name}:`);
  output.appendLine(JSON.stringify(res.data?.inputs ?? {}, null, 2));
  output.show(true);
}

async function abort(views: Views, node?: { instance: AcmInstance; execution: Execution }): Promise<void> {
  const instance = node?.instance ?? running?.instance;
  const executionId = node?.execution.id ?? running?.executionId;
  if (!instance || !executionId) {
    vscode.window.showInformationMessage('ACM: No execution is running.');
    return;
  }
  if (instance.readonly) {
    vscode.window.showErrorMessage(`ACM: Instance "${instance.name}" is read-only, abort is blocked.`);
    return;
  }
  const client = node ? await getClient(instance) : running?.client;
  if (!client) {
    return;
  }
  await client.request('DELETE', `${ACM_API.queueCode}?executionId=${encodeURIComponent(executionId)}`);
  vscode.window.setStatusBarMessage(`ACM: Abort requested for ${executionId}`, 5000);
  views.refreshExecutions();
}

async function selectInstance(): Promise<void> {
  const instance = await pickInstance();
  if (instance) {
    await setActiveInstance(instance.name);
  }
}

async function setCredentialsCommand(): Promise<void> {
  const instance = getActiveInstance() ?? (await pickInstance('Select instance to set credentials for'));
  if (instance && (await setCredentials(instance))) {
    vscode.window.showInformationMessage(`ACM: Credentials for "${instance.name}" saved.`);
  }
}

async function checkConnection(): Promise<void> {
  const target = await getTarget();
  if (!target) {
    return;
  }
  await target.client.request('GET', ACM_API.state);
  vscode.window.showInformationMessage(
    `ACM: Connected to "${target.instance.name}" (${target.instance.url}) using ${target.client.authDescription}.`,
  );
}

function showError(error: unknown): void {
  vscode.window.showErrorMessage(`ACM: ${error instanceof Error ? error.message : String(error)}`);
}
