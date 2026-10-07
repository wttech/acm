# ACM Tools

Developer tools for [AEM Content Manager (ACM)](../README.md), released separately from ACM.

| Tool | Path | Distribution | Tag |
|---|---|---|---|
| MCP server | [mcp-server](mcp-server/README.md) | npm `@wppes/acm-mcp-server` + MCP Registry | `mcp-server-v<version>` |
| VS Code extension | [vscode-extension](vscode-extension/README.md) | VS Code Marketplace and Open VSX (`wppes.acm`), `.vsix` on GitHub Releases; bundles the MCP server | `vscode-extension-v<version>` |

ACM itself is tagged `v<version>`.

To build, check or release the tools, see the [development guide](DEVELOPMENT.md).

## Skills

[acm-groovy-script](skills/acm-groovy-script/SKILL.md) is the single source of guidance for agents writing ACM scripts. It is distributed through:

- the MCP server: its essentials as server instructions, the full guide as the `acm-groovy-script` prompt, and all documents as `acm://skill/...` resources;
- the VS Code extension: contributed to Copilot with `chatSkills`;
- a plain copy of the folder into `.github/skills/`, `.claude/skills/` or `.agents/skills/` for other agents.

`evals/` holds prompts and expected results for checking the skill with an agent; it is not shipped.

