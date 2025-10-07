import { ScriptRoot } from './script';

export const ConsoleDefaultScriptPath = `${ScriptRoot}/template/core/console.groovy`;
export const ConsoleDefaultScriptContent = `
boolean canRun() {
  return conditions.always()
}
  
void doRun() {
  println "Hello World!"
}
`.trim();
