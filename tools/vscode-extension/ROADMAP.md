# Roadmap

Status: `[x]` done, `[~]` stub or partial, `[ ]` not started.

## Foundation

- [x] Skeleton: commands, settings, views, esbuild bundle with `@acm/shared`, `.vsix` packaging, CI.
- [x] Shared ACM client: `AcmClient` (bearer, cookie, basic auth, CSRF, 401/403 messages), execution helpers and `normalizeGroovy` live in `tools/shared`; the MCP server bundles them with esbuild.
- [x] Credentials: `ACM: Set Credentials` stores the token, cookie or password in `context.secrets`, keyed by instance name and URL; `getClient()` builds an `AcmClient`.
- [x] Tests: `@vscode/test-cli` with a smoke test, run in CI under `xvfb-run`.
- [x] Lint: ESLint with `typescript-eslint`.

## Features

### Running

- [x] **Ad-hoc execution.** `ACM: Run Script` / `Run Selection` queues code (`queue-code`), polls until done and streams console output to an output channel. Status bar shows the running execution; `ACM: Abort Execution` or cancelling the progress aborts it. `ACM: Run Without History` (`execute-code`, `history: false`) is offered only with ACM's `console/execute/nohistory` feature.
- [~] **Inputs form.** Before running a script with `describeRun()`, inputs are resolved (`describe-code`) and asked for with quick picks, input boxes and file pickers (uploaded via `file`). Next: a webview form.
- [x] **Instance guardrails.** `readonly` instances block running; any non-local instance asks for confirmation before running.

### Editing

- [~] **Completion.** Lifecycle methods as snippets, script variables, and the methods of each variable (`repo.`, `acl.`, `inputs.`, …) from the API generated from the ACM source. Next: chained calls (`repo.get(…).`), options inside input and output closures; dynamic suggestions from `assist-code` (Java classes, variables, snippets, JCR paths in strings).
- [~] **Inline docs.** Hover for lifecycle methods, variables and their methods. Next: Java classes from `assist-code`, links to ACM docs.
- [~] **Validation.** On save of scripts declaring `doRun()`, compile-check on the active instance (`execute-code`, `mode=parse`) and show errors as diagnostics with line and column; missing or misspelled lifecycle methods are reported by ACM. Next: local checks without an instance.
- [x] **CodeLens.** `Run` above `doRun`; validation runs on save and inputs are asked for on run, so no other lenses.
- [~] **Snippets.** `acmdoc` inserts the script documentation header. Next: snippets from the instance (`snippet`).
- [x] **New script.** `File > New File... > ACM Script` opens a documented script from the skill's patterns (minimal, migration, ACL, report, scheduled cleanup, console, mock).
- [ ] **JCR path links.** Ctrl+click on `/content/...` or `/conf/...` opens CRXDE or Sites on the active instance.

### Browsing

- [x] **Executions view.** History and queue (`execution`), console output as a read-only document, abort, download of outputs and the ZIP archive (`execution-output`).
- [x] **Scripts view.** Scripts stored on the instance (`script`), open read-only, diff with the local file.
- [x] **Instances.** Status bar switcher for the active instance with its health (`state`, every minute); `ACM: Check Connection`.

### AI

- [x] **Scripting skill.** The ACM Groovy scripting skill from `tools/skills` is contributed to Copilot with `chatSkills`.
- [x] **MCP server provider.** The MCP server is bundled (`dist/mcp-server.mjs`, run by the editor's Node.js) and registered for the active instance with `vscode.lm.registerMcpServerDefinitionProvider`; secrets are added only when it starts. Requires VS Code 1.101+.
- [x] **MCP for other tools.** `ACM: Copy MCP Setup Prompt` copies a prompt for the active instance (no secrets) that any AI agent follows to register the standalone MCP server; no per-tool config formats are documented or generated, so nothing goes stale.

### Onboarding

- [x] **Walkthrough.** Connect to AEM, write a documented script, run it, let an agent help.

## Distribution

- [x] `.vsix` on GitHub Releases, installed with `Extensions: Install from VSIX...`.
- [~] VS Code Marketplace (`vsce publish`) under the `wppes` publisher: workflow ready, needs the publisher and the `VSCE_PAT` secret.
- [~] Open VSX (`ovsx publish`) for Cursor, Windsurf and VSCodium: workflow ready, needs the `wppes` namespace and the `OVSX_PAT` secret.
