import * as vscode from 'vscode';
import { ACM_API, CONSOLE_CODE_ID, type AcmClient } from '@acm/shared';

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
export async function promptInputs(client: AcmClient, content: string): Promise<Record<string, unknown> | undefined> {
  const res = await client.request<{ inputs?: Record<string, InputDefinition> }>('POST', ACM_API.describeCode, {
    code: { id: CONSOLE_CODE_ID, content },
  });
  const inputs = Object.values(res.data?.inputs ?? {});
  const values: Record<string, unknown> = {};
  for (const [index, input] of inputs.entries()) {
    const value = await promptInput(input, `${input.label || input.name} (${index + 1}/${inputs.length})`);
    if (value === CANCELLED) {
      return undefined;
    }
    values[input.name] = value;
  }
  return values;
}

async function promptInput(input: InputDefinition, title: string): Promise<unknown> {
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
    case 'STRING':
    case 'TEXT':
    case 'PATH':
    case 'COLOR':
    case 'DATE':
    case 'TIME':
    case 'DATETIME': {
      const text = await showInput(input, title);
      return text === undefined ? CANCELLED : text;
    }
    case 'FILE':
    case 'MULTIFILE':
      // File upload is not supported here, so the declared default is sent.
      return input.value ?? null;
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
