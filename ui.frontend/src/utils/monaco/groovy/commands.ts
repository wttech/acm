import { Monaco } from '@monaco-editor/react';
import { registerImportClass } from './commands/import-class.ts';

export function registerCommands(instance: Monaco) {
  registerImportClass(instance);
}
