# ACM for VS Code

Write, validate and run [AEM Content Manager (ACM)](https://github.com/wttech/acm) Groovy scripts from VS Code.

> [!NOTE]
> Early preview. See the [roadmap](ROADMAP.md).

The extension also contributes the [ACM Groovy scripting skill](../skills/acm-groovy-script/SKILL.md) to Copilot, so agent mode writes ACM scripts with the real API and safe defaults.

## Installation

Install **AEM Content Manager (ACM)** (`wppes.acm`) from the VS Code Marketplace, or from Open VSX in Cursor, Windsurf and VSCodium.

To install a specific build, download `acm-<version>.vsix` from the [releases](https://github.com/wttech/acm/releases?q=vscode-extension) (or from the `acm-vscode-extension` artifact of a CI run) and run `Extensions: Install from VSIX...`.

## Configuration

Works out of the box with a local AEM SDK: the default instance `author` is `http://localhost:4502` with `admin`/`admin`.

For other instances, add them to the workspace settings (`.vscode/settings.json`), then run `ACM: Select Instance` and `ACM: Set Credentials`:

```json
"acm.instances": [
  { "name": "local", "url": "http://localhost:4502", "authMode": "basic", "user": "admin" },
  { "name": "dev", "url": "https://author-pXXXX-eYYYY.adobeaemcloud.com", "authMode": "bearer", "readonly": true }
]
```

Tokens, cookies and passwords are kept in VS Code secret storage, never in settings, and are bound to the instance URL. Running on a `readonly` instance is blocked; running on any non-local instance asks for confirmation. The active instance is shown in the status bar; click it to switch.

## Usage

- `File > New File... > ACM Script` (or `ACM: New Script...`) starts from a documented template: minimal, content migration, ACL setup, CSV report, scheduled cleanup, console or HTTP mock. Type `acmdoc` to add the documentation header shown in the ACM UI to an existing script.
- `ACM: Run Script` (also the play button and the `Run` CodeLens above `doRun()`) queues the script, asks for inputs declared in `describeRun()` and streams console output to the `ACM` output channel. Bare snippets are wrapped in `canRun()`/`doRun()`. Cancel the progress notification or click the status bar to abort.
- `ACM: Validate Script` compile-checks the script on the active instance and shows errors in Problems; with `acm.validateOnSave`, scripts declaring `doRun()` are checked on save.
- The ACM activity bar lists executions (click for the log) and scripts stored on the instance (open read-only or compare with a local file).

## Development

```shell
cd tools/vscode-extension
npm install
npm run watch       # then press F5 in VS Code with this folder open
npm run typecheck
npm run lint
npm test            # smoke test in a downloaded VS Code
npm run package     # builds acm-<version>.vsix
```

Shared ACM domain code lives in [tools/shared](../shared) and is bundled in as `@acm/shared`; see [tools/README.md](../README.md).
