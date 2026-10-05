# ACM Tools

Developer tools for [AEM Content Manager (ACM)](../README.md), released separately from ACM.

| Tool | Path | Distribution | Tag |
|---|---|---|---|
| MCP server | [mcp-server](mcp-server/README.md) | npm `@wppes/acm-mcp-server` + MCP Registry | `mcp-server-v<version>` |
| VS Code extension | [vscode-extension](vscode-extension/README.md) | `.vsix` on GitHub Releases | `vscode-extension-v<version>` |

ACM itself is tagged `v<version>`.

## Layout

```
tools/
  shared/             # ACM domain in TypeScript: API paths, types, client, DSL catalog. Not a package.
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
- Keep `catalog/` in sync with the Groovy API in `core/` (`ContentScriptSyntax`, `Inputs`, `Outputs`, `CodeContext`, `ExecutionContext`).

## CI

[Check](../.github/workflows/check.yml) runs on every pull request and push to `main`. It detects changed paths and runs only the affected jobs, so one pull request can touch ACM and any tool:

- `ACM`: Maven build, for changes outside `tools/`.
- `MCP Server`: tests on Node 22 and 24, for `tools/mcp-server/**` and `tools/shared/**`.
- `VS Code Extension`: type check and packaging, for `tools/vscode-extension/**` and `tools/shared/**`. The `.vsix` is uploaded as a build artifact.

## Releasing

Each tool has its own version and release workflow triggered by its tag prefix. Bump the version in the tool's `package.json` and `package-lock.json` with `npm version <version> --no-git-tag-version`, merge to `main`, then tag the merge commit:

```shell
git tag vscode-extension-v<version>
git push origin vscode-extension-v<version>
```

- [MCP Server workflow](../.github/workflows/mcp-server.yml): publishes to npm and the MCP Registry. See the [MCP server release notes](mcp-server/README.md#releasing).
- [VS Code Extension workflow](../.github/workflows/vscode-extension.yml): creates a GitHub release with the `.vsix`, not marked as the latest release.
