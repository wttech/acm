import * as path from 'node:path';
import * as vscode from 'vscode';
import { ACM_API, type AcmClient } from '@acm/shared';
import { CONTEXT } from './ids';

interface InputDefinition {
  name: string;
  type: string;
  value?: unknown;
  label?: string;
  description?: string;
  required?: boolean;
  options?: Record<string, unknown>;
}

const CANCELLED = Symbol('cancelled');

interface InputSession {
  doc: vscode.TextDocument;
  inputs: InputDefinition[];
  diagnostics: vscode.DiagnosticCollection;
  complete: (values?: Record<string, unknown>) => void;
}

// Keyed by document URI, so several scripts can have their inputs edited at the same time.
const inputSessions = new Map<string, InputSession>();

function updateEditingInputsContext(): Thenable<unknown> {
  const active = vscode.window.activeTextEditor?.document.uri.toString();
  return vscode.commands.executeCommand('setContext', CONTEXT.editingInputs, !!active && inputSessions.has(active));
}

/** Resolves inputs declared in `describeRun()` and asks for their values; `undefined` when cancelled. */
export async function promptInputs(
  client: AcmClient,
  code: { id: string; content?: string },
  label?: string,
): Promise<Record<string, unknown> | undefined> {
  const res = await client.request<{ inputs?: Record<string, InputDefinition> }>('POST', ACM_API.describeCode, {
    code,
  });
  const inputs = Object.values(res.data?.inputs ?? {});
  if (!inputs.length) {
    return {};
  }
  const values = Object.fromEntries(inputs.map((input) => [input.name, initialValue(input)]));
  for (const input of inputs.filter(({ type }) => type === 'FILE' || type === 'MULTIFILE')) {
    const value = await promptFiles(client, input);
    if (value === CANCELLED) {
      return undefined;
    }
    values[input.name] = value;
  }
  return showInputsEditor(inputs, values, label);
}

async function promptFiles(client: AcmClient, input: InputDefinition): Promise<unknown> {
  const multiple = input.type === 'MULTIFILE';
  const label = input.label || input.name;
  const files = await vscode.window.showOpenDialog({
    title: label,
    canSelectMany: multiple,
    openLabel: `Upload for '${label}'`,
  });
  if (!files) {
    return input.required ? CANCELLED : multiple ? [] : null;
  }
  const paths = await uploadFiles(client, files);
  return multiple ? paths : paths[0];
}

/** Untitled documents are identified by their URI, so a second input editor with the same title would reuse the first. */
function uniqueUntitledUri(title: string): vscode.Uri {
  const open = new Set(
    vscode.workspace.textDocuments.filter((doc) => doc.uri.scheme === 'untitled').map((doc) => doc.uri.path),
  );
  const base = vscode.Uri.file(title).path;
  if (!open.has(base)) {
    return vscode.Uri.file(title).with({ scheme: 'untitled' });
  }
  const extensionIndex = title.lastIndexOf('.json');
  for (let n = 2; ; n++) {
    const candidate = `${title.slice(0, extensionIndex)} (${n}).json`;
    if (!open.has(vscode.Uri.file(candidate).path)) {
      return vscode.Uri.file(candidate).with({ scheme: 'untitled' });
    }
  }
}

async function showInputsEditor(
  inputs: InputDefinition[],
  initialValues: Record<string, unknown>,
  label?: string,
): Promise<Record<string, unknown> | undefined> {
  const uri = uniqueUntitledUri(label ? `${label} — inputs.json` : 'inputs.json');
  const doc = await vscode.workspace.openTextDocument(uri);
  const editor = await vscode.window.showTextDocument(doc, { preview: false });
  await editor.edit((editBuilder) => {
    editBuilder.insert(new vscode.Position(0, 0), JSON.stringify(initialValues, null, 2));
  });
  const diagnostics = vscode.languages.createDiagnosticCollection('acmInputs');
  const key = doc.uri.toString();
  const disposables: vscode.Disposable[] = [];
  try {
    return await new Promise<Record<string, unknown> | undefined>((resolve) => {
      const complete = (values?: Record<string, unknown>) => resolve(values);
      inputSessions.set(key, { doc, inputs, diagnostics, complete });
      disposables.push(
        vscode.window.onDidChangeActiveTextEditor(updateEditingInputsContext),
        vscode.workspace.onDidCloseTextDocument((closed) => closed === doc && complete()),
      );
      void updateEditingInputsContext();
    });
  } finally {
    inputSessions.delete(key);
    disposables.forEach((disposable) => disposable.dispose());
    await updateEditingInputsContext();
    diagnostics.dispose();
    if (!doc.isClosed) {
      await vscode.window.showTextDocument(doc, { preview: false });
      await vscode.commands.executeCommand('workbench.action.revertAndCloseActiveEditor');
    }
  }
}

export async function confirmInputs(): Promise<void> {
  const active = vscode.window.activeTextEditor?.document;
  const session = active && inputSessions.get(active.uri.toString());
  if (!session) {
    return;
  }
  const result = parseInputs(session.doc, session.inputs);
  session.diagnostics.set(session.doc.uri, result.diagnostics);
  if (result.values) {
    session.complete(result.values);
  } else {
    vscode.window.showWarningMessage('ACM: Fix the script input errors before running.');
  }
}

function parseInputs(
  doc: vscode.TextDocument,
  inputs: InputDefinition[],
): { values?: Record<string, unknown>; diagnostics: vscode.Diagnostic[] } {
  let parsed: unknown;
  try {
    parsed = JSON.parse(doc.getText());
  } catch (error) {
    return { diagnostics: [diagnostic(doc, error instanceof Error ? error.message : 'Enter valid JSON.')] };
  }
  if (!isObject(parsed)) {
    return { diagnostics: [diagnostic(doc, 'Script inputs must be a JSON object.')] };
  }
  const definitions = new Map(inputs.map((input) => [input.name, input]));
  const diagnostics: vscode.Diagnostic[] = [];
  for (const name of Object.keys(parsed)) {
    if (!definitions.has(name)) {
      diagnostics.push(diagnostic(doc, `Unknown script input "${name}".`, name));
    }
  }
  for (const input of inputs) {
    const value = parsed[input.name];
    const message = validateInput(input, value, Object.prototype.hasOwnProperty.call(parsed, input.name));
    if (message) {
      diagnostics.push(diagnostic(doc, `${input.label || input.name}: ${message}`, input.name));
    }
  }
  return diagnostics.length ? { diagnostics } : { values: parsed, diagnostics };
}

function initialValue(input: InputDefinition): unknown {
  if (input.type === 'NUMBER_RANGE' && isObject(input.value)) {
    return [input.value.start ?? null, input.value.end ?? null];
  }
  return input.value ?? null;
}

function validateInput(input: InputDefinition, value: unknown, present: boolean): string | undefined {
  if (!present || value === null || value === '') {
    return input.required ? 'Value is required.' : undefined;
  }
  switch (input.type) {
    case 'BOOL':
      return typeof value === 'boolean' ? undefined : 'Enter a boolean.';
    case 'INTEGER':
      return typeof value === 'number' && Number.isInteger(value) ? undefined : 'Enter an integer.';
    case 'DECIMAL':
      return typeof value === 'number' && Number.isFinite(value) ? undefined : 'Enter a number.';
    case 'MULTISELECT':
      return Array.isArray(value) &&
        value.every((item) => Object.values(input.options ?? {}).some((option) => Object.is(option, item)))
        ? undefined
        : 'Enter an array containing only available values.';
    case 'MULTIFILE':
      return Array.isArray(value) && value.every((item) => typeof item === 'string')
        ? undefined
        : 'Enter an array of strings.';
    case 'MAP':
      return isObject(value) ? undefined : 'Enter an object.';
    case 'KEY_VALUE_LIST':
      return isObject(value) || Array.isArray(value) ? undefined : 'Enter an object or an array.';
    case 'NUMBER_RANGE':
      return Array.isArray(value) &&
        value.length === 2 &&
        value.every((item) => item === null || (typeof item === 'number' && Number.isFinite(item)))
        ? undefined
        : 'Enter an array of two numbers or null bounds.';
    case 'SELECT':
      return Object.values(input.options ?? {}).some((option) => Object.is(option, value))
        ? undefined
        : 'Choose one of the available values.';
    case 'STRING':
    case 'TEXT':
    case 'PATH':
    case 'COLOR':
    case 'DATE':
    case 'TIME':
    case 'DATETIME':
    case 'FILE':
      return typeof value === 'string' ? undefined : 'Enter a string.';
    default:
      return undefined;
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function diagnostic(doc: vscode.TextDocument, message: string, name?: string): vscode.Diagnostic {
  const text = doc.getText();
  const key = name ? JSON.stringify(name) : undefined;
  const start = key ? text.indexOf(key) : -1;
  const range =
    start >= 0 && key
      ? new vscode.Range(doc.positionAt(start), doc.positionAt(start + key.length))
      : new vscode.Range(doc.positionAt(0), doc.positionAt(text.length));
  const diagnostic = new vscode.Diagnostic(range, message, vscode.DiagnosticSeverity.Error);
  diagnostic.source = 'ACM';
  return diagnostic;
}

/** Uploads files to ACM's temporary storage; file inputs take the returned repository paths. */
async function uploadFiles(client: AcmClient, files: vscode.Uri[]): Promise<string[]> {
  const form = new FormData();
  for (const file of files) {
    const name = path.basename(file.fsPath);
    form.append(name, new Blob([new Uint8Array(await vscode.workspace.fs.readFile(file))]), name);
  }
  const res = await client.request<{ files?: string[] }>('POST', ACM_API.file, form);
  return res.data?.files ?? [];
}
