# ACM Tools

Developer tools for [AEM Content Manager (ACM)](../README.md), released separately from ACM.

| Tool | Path | Distribution | Tag |
|---|---|---|---|
| MCP server | [mcp-server](mcp-server/README.md) | npm `@wppes/acm-mcp-server` + MCP Registry | `mcp-server-v<version>` |
| VS Code extension | [vscode-extension](vscode-extension/README.md) | VS Code Marketplace and Open VSX (`wppes.acm`), `.vsix` on GitHub Releases; bundles the MCP server | `vscode-extension-v<version>` |

ACM itself is tagged `v<version>`.

## Layout

```
tools/
  shared/             # ACM domain in TypeScript: API paths, types, client, script API catalog. Not a package.
  skills/             # Agent Skills (agentskills.io), bundled into the tools
  mcp-server/         # own package.json, lock file and version
  vscode-extension/   # own package.json, lock file and version
```

## Sharing code

`shared/` is source code, not a library. It has no `package.json`, no dependencies and is never published or versioned.
Each tool imports it as `@acm/shared` through `paths` in its `tsconfig.json`, and its bundler (esbuild) compiles it into the tool's output.
So every tool ships as a single self-contained package, and a change in `shared/` takes effect in a tool on the tool's next release.

Rules for `shared/`:

- No runtime dependencies and no Node- or VS Code-specific APIs; use globals available in both (`fetch`, `URL`, `AbortController`).
- No configuration loading. Tools pass configuration in (environment variables in the MCP server, settings and secret storage in VS Code).

## Generated script API

[codegen.mjs](shared/codegen.mjs) reads the Java sources in `core/` and writes the API available to Groovy scripts (variables, script methods, inputs, outputs and the public methods of the related classes):

- [shared/src/catalog/api.json](shared/src/catalog/api.json), used for completion and hover in the VS Code extension;
- [skills/acm-groovy-script/references/api.md](skills/acm-groovy-script/references/api.md), the API reference agents read.

It is a dependency-free Node script that runs on every `ui.frontend` build, so a regular Maven build keeps both files current. To run it alone:

```shell
node tools/shared/codegen.mjs
```

Commit the regenerated files together with the Java change. Prose (docs and snippets for script methods and variables) lives in [entries.ts](shared/src/catalog/entries.ts) and the skill.

## Skills

[acm-groovy-script](skills/acm-groovy-script/SKILL.md) is the single source of guidance for agents writing ACM scripts. It is distributed through:

- the MCP server: its essentials as server instructions, the full guide as the `acm-groovy-script` prompt, and all documents as `acm://skill/...` resources;
- the VS Code extension: contributed to Copilot with `chatSkills`;
- a plain copy of the folder into `.github/skills/`, `.claude/skills/` or `.agents/skills/` for other agents.

`evals/` holds prompts and expected results for checking the skill with an agent; it is not shipped.

## CI

[Check](../.github/workflows/check.yml) runs on every pull request and push to `main`. It detects changed paths and runs only the affected jobs, so one pull request can touch ACM and any tool:

- `ACM`: Maven build, for changes outside `tools/`.
- `Codegen`: regenerates the script API and fails if the committed files are outdated. Always runs.
- `MCP Server`: tests on Node 22 and 24, for `tools/mcp-server/**`, `tools/shared/**` and `tools/skills/**`.
- `VS Code Extension`: type check, lint, manifest check, smoke test in VS Code (under `xvfb-run`) and packaging, for `tools/vscode-extension/**`, `tools/mcp-server/**` (bundled), `tools/shared/**` and `tools/skills/**`. The `.vsix` is uploaded as a build artifact.

Run `npm run check` in `tools/mcp-server` or `tools/vscode-extension` to run the same steps as the matching CI job before pushing (the extension's smoke test opens a VS Code window).

## Releasing

Each tool has its own version and release workflow triggered by its tag prefix. `sh taskw release:mcp-server -- <version>` / `sh taskw release:vscode-extension -- <version>` (run from the repository root) bump the version in the tool's `package.json` and `package-lock.json` with `npm version <version> --no-git-tag-version` (and, for the MCP server, both `version` fields in `server.json`), then commit, push, tag and push the tag.

- [MCP Server workflow](../.github/workflows/release.mcp-server.yml): publishes to npm and the MCP Registry. See the [MCP server release notes](mcp-server/README.md#releasing).
- [VS Code Extension workflow](../.github/workflows/release.vscode-extension.yml): creates a GitHub release with the `.vsix`, not marked as the latest release, then publishes it to the VS Code Marketplace and Open VSX under the `wppes` publisher. Each publish step runs only when its token is set as a repository secret: `VSCE_PAT` (Azure DevOps token with the *Marketplace: Manage* scope, for the `wppes` publisher) and `OVSX_PAT` (Open VSX token of a member of the `wppes` namespace). Add release notes to [CHANGELOG.md](vscode-extension/CHANGELOG.md) before running the task.

