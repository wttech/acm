import { ScriptRoot } from './script';

export type Executable = {
  id: string;
  content: string;
  metadata: ExecutableMetadata;
};

export type ExecutableMetadata = Record<string, string | string[]>

export const ExecutableIdConsole = 'console';

export function isExecutableConsole(id: string): boolean {
  return id === ExecutableIdConsole;
}

export function isExecutableScript(id: string): boolean {
  return id.startsWith(ScriptRoot);
}

