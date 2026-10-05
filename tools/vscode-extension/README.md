# ACM for VS Code

Write, validate and run [AEM Content Manager (ACM)](https://github.com/wttech/acm) Groovy scripts from VS Code.

> [!NOTE]
> Early preview. Code completion and inline docs for the ACM script API work offline; most commands are stubs. See the [roadmap](ROADMAP.md).

The extension also contributes the [ACM Groovy scripting skill](../skills/acm-groovy-script/SKILL.md) to Copilot, so agent mode writes ACM scripts with the real API and safe defaults.

## Installation

1. Download `acm-<version>.vsix` from the [releases](https://github.com/wttech/acm/releases?q=vscode-extension) (or from the `acm-vscode-extension` artifact of a CI run).
2. In VS Code, run `Extensions: Install from VSIX...` and pick the file.

## Configuration

Add instances in settings, then run `ACM: Select Instance` and `ACM: Set Credentials`:

```json
"acm.instances": [
  { "name": "local", "url": "http://localhost:4502", "authMode": "basic", "user": "admin" },
  { "name": "dev", "url": "https://author-pXXXX-eYYYY.adobeaemcloud.com", "authMode": "bearer", "readonly": true }
]
```

Tokens, cookies and passwords are kept in VS Code secret storage, never in settings.

## Development

```shell
cd tools/vscode-extension
npm install
npm run watch       # then press F5 in VS Code with this folder open
npm run typecheck
npm run package     # builds acm-<version>.vsix
```

Shared ACM domain code lives in [tools/shared](../shared) and is bundled in as `@acm/shared`; see [tools/README.md](../README.md).
