# VS Code extension development

How to build, check and release the ACM VS Code extension. For using it, see the [README](README.md).

## Building and running

```shell
cd tools/vscode-extension
npm install
npm run watch                                      # rebuild on change
code --extensionDevelopmentPath="$PWD" ../..       # editor with the extension loaded
npm run check                                      # what CI runs: typecheck, lint, manifest, test, package
npm run package                                    # builds dist/acm-<version>.vsix
```

`npm test` runs a smoke test in a VS Code window. The bundled MCP server is built from [tools/mcp-server](../mcp-server) sources, and shared ACM code from [tools/shared](../shared) is bundled in as `@acm/shared`; see the [tools development guide](../DEVELOPMENT.md).

## Conventions

- **Identifiers have one source.** Command, view, context key, setting and tree item names live in `src/ids.ts`; `npm run check:manifest` fails when they differ from the contributions in `package.json`. Setting defaults live only in `package.json`, read through `readSetting`. API paths, MCP environment variables and ACM feature IDs come from `@acm/shared`.
- **UI and logic stay apart.** Anything that does not need `vscode` (queries, filters, polling, formatting) belongs in `tools/shared` and is used by both tools; the extension only wires it to views, commands and prompts.
- **Settings are explicit.** Everything users may need to tune is a setting with a description saying what the value bounds and what happens when it is exceeded; it is passed to the bundled MCP server too. Secrets go to VS Code secret storage only.
- **Native APIs only.** Raise `engines.vscode` instead of feature-detecting APIs.

See also the [roadmap](ROADMAP.md) and the [changelog](CHANGELOG.md).

## Releasing

From the repository root, with a clean working tree and release notes in [CHANGELOG.md](CHANGELOG.md):

```shell
sh taskw release:vscode-extension -- <version>
```

The task bumps the version in `package.json` and `package-lock.json` (do not edit them by hand first), commits, pushes, tags `vscode-extension-v<version>` and pushes the tag. The release workflow publishes the `.vsix` to GitHub Releases, the VS Code Marketplace and Open VSX; see the [tools development guide](../DEVELOPMENT.md#releasing).
