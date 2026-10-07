import * as path from 'node:path';
import * as vscode from 'vscode';
import { ACM_API, type AcmClient } from '@acm/shared';

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

/** Resolves inputs declared in `describeRun()` and asks for their values; `undefined` when cancelled. */
export async function promptInputs(
  client: AcmClient,
  code: { id: string; content?: string },
): Promise<Record<string, unknown> | undefined> {
  const res = await client.request<{ inputs?: Record<string, InputDefinition> }>('POST', ACM_API.describeCode, {
    code,
  });
  const inputs = Object.values(res.data?.inputs ?? {});
  const values: Record<string, unknown> = {};
  for (const [index, input] of inputs.entries()) {
    const value = await promptInput(client, input, `${input.label || input.name} (${index + 1}/${inputs.length})`);
    if (value === CANCELLED) {
      return undefined;
    }
    values[input.name] = value;
  }
  return values;
}

async function promptInput(client: AcmClient, input: InputDefinition, title: string): Promise<unknown> {
  const prompt = input.description || undefined;
  switch (input.type) {
    case 'BOOL': {
      const items = input.value ? ['true', 'false'] : ['false', 'true'];
      const picked = await vscode.window.showQuickPick(items, { title, placeHolder: prompt, ignoreFocusOut: true });
      return picked === undefined ? CANCELLED : picked === 'true';
    }
    case 'SELECT': {
      const items = optionItems(input);
      items.sort((a, b) => Number(b.value === input.value) - Number(a.value === input.value));
      const picked = await vscode.window.showQuickPick(items, { title, placeHolder: prompt, ignoreFocusOut: true });
      return picked === undefined ? CANCELLED : picked.value;
    }
    case 'MULTISELECT': {
      const selected = Array.isArray(input.value) ? input.value : [];
      const items = optionItems(input).map((item) => ({ ...item, picked: selected.includes(item.value) }));
      const picked = await vscode.window.showQuickPick(items, {
        title,
        placeHolder: prompt,
        canPickMany: true,
        ignoreFocusOut: true,
      });
      return picked === undefined ? CANCELLED : picked.map((item) => item.value);
    }
    case 'INTEGER':
    case 'DECIMAL': {
      const text = await showInput(input, title, (value) =>
        value.trim() === '' || Number.isFinite(Number(value)) ? undefined : 'Enter a number.',
      );
      return text === undefined ? CANCELLED : text.trim() === '' ? null : Number(text);
    }
    case 'TEXT': {
      const text = await showTextEditor(input, title);
      return text === undefined ? CANCELLED : text;
    }
    case 'STRING':
    case 'PATH':
    case 'COLOR':
    case 'DATE':
    case 'TIME':
    case 'DATETIME': {
      const text = await showInput(input, title);
      return text === undefined ? CANCELLED : text;
    }
    case 'FILE':
    case 'MULTIFILE': {
      const multiple = input.type === 'MULTIFILE';
      const files = await vscode.window.showOpenDialog({ title, canSelectMany: multiple, openLabel: 'Upload' });
      if (!files) {
        return input.required ? CANCELLED : multiple ? [] : null;
      }
      const paths = await uploadFiles(client, files);
      return multiple ? paths : paths[0];
    }
    default: {
      const text = await showInput(input, `${title} (JSON)`, (value) => {
        try {
          JSON.parse(value || 'null');
          return undefined;
        } catch {
          return 'Enter valid JSON.';
        }
      });
      return text === undefined ? CANCELLED : JSON.parse(text || 'null');
    }
  }
}

function optionItems(input: InputDefinition): Array<vscode.QuickPickItem & { value: unknown }> {
  return Object.entries(input.options ?? {}).map(([label, value]) => ({
    label,
    description: String(value) === label ? undefined : String(value),
    value,
  }));
}

function showInput(
  input: InputDefinition,
  title: string,
  validate?: (value: string) => string | undefined,
): Thenable<string | undefined> {
  const value = input.value;
  return vscode.window.showInputBox({
    title,
    prompt: input.description || undefined,
    value: value === undefined || value === null ? '' : typeof value === 'string' ? value : JSON.stringify(value),
    ignoreFocusOut: true,
    validateInput: (text) =>
      input.required && text.trim() === '' ? 'Value is required.' : validate?.(text),
  });
}

/** Input boxes are single-line, so multi-line text is edited in an editor and confirmed with a picker. */
async function showTextEditor(input: InputDefinition, title: string): Promise<string | undefined> {
  const value = input.value;
  const doc = await vscode.workspace.openTextDocument({
    language: 'plaintext',
    content: value === undefined || value === null ? '' : String(value),
  });
  await vscode.window.showTextDocument(doc, { preview: false });
  try {
    for (;;) {
      const picked = await vscode.window.showQuickPick(
        [
          { label: '$(check) Confirm', description: 'Use the text from the editor', confirm: true, alwaysShow: true },
          { label: '$(close) Cancel', confirm: false, alwaysShow: true },
        ],
        {
          title,
          placeHolder: [input.description, 'Edit the text in the editor, then confirm'].filter(Boolean).join(' - '),
          ignoreFocusOut: true,
        },
      );
      if (!picked?.confirm) {
        return undefined;
      }
      const text = doc.getText();
      if (!input.required || text.trim() !== '') {
        return text;
      }
      vscode.window.showWarningMessage(`${title}: value is required.`);
    }
  } finally {
    if (!doc.isClosed) {
      await vscode.window.showTextDocument(doc, { preview: false });
      await vscode.commands.executeCommand('workbench.action.revertAndCloseActiveEditor');
    }
  }
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
