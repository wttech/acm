# AEM Content Manager (ACM) for VS Code

Write, validate and run [AEM Content Manager (ACM)](https://github.com/wttech/acm) Groovy scripts on Adobe Experience Manager without leaving the editor. The ACM MCP server is registered automatically for the active instance, so AI agents can validate and run scripts, follow executions and read outputs on your AEM right after installing, with no configuration.

Using another AI tool or editor? Run **ACM: Copy MCP Setup Prompt** and paste it into the agent: it sets up the same MCP server by itself.

Works with AEM as a Cloud Service, AEM 6.5 and AMS, wherever ACM is installed.

> **Preview.** Feedback and issues are welcome on [GitHub](https://github.com/wttech/acm/issues).

## Features

- **Zero-config start.** A local AEM SDK at `http://localhost:4502` with `admin`/`admin` works right after installing, together with the ACM MCP server for AI agents and the ACM scripting skill (see [AI agents](#ai-agents)). The *Get Started with ACM* walkthrough shows the rest.
- **Run scripts and selections** on the active instance. Inputs declared in `describeRun()` are asked for (files are uploaded, multi-line text is edited in an editor), console output streams into the `ACM` output channel, and a running script can be aborted. Manual scripts run from the Instance Scripts view, or from their opened editor, run by ID: the instance's stored version executes and its executions are listed under the script.
- **Run without history** while iterating on read-only code, for users with ACM's `console/execute/nohistory` permission. Such runs are still traced in ACM's [audit log](https://github.com/wttech/acm#audit-log).
- **Compile errors on save.** Scripts are checked by ACM on the active instance and errors show up in Problems.
- **Code completion and docs** for the whole ACM script API (`repo`, `acl`, `inputs`, `outputs`, `conditions`, …), generated from the ACM source.
- **Documented templates.** `File > New File... > ACM Script` starts from a template: content migration, ACL setup, CSV report, scheduled cleanup, console code or HTTP mock. Type `acmdoc` to add the documentation header shown in the ACM UI.
- **Project, instance and execution views.** *Project Scripts* lists the scripts of the project's content package (found automatically or set in `acm.scripts.root`) and creates new ones from templates with a right click on a type, with duplicate, rename, delete and reveal in Explorer next to run and compare; *Instance Scripts* lists what is deployed, named and grouped like in the ACM UI; *Executions* browses the history with logs and downloadable outputs and filters it by script or status. Compare a script with its counterpart on the other side from the context menu.
- **Instance status** in the status bar: switch instances in one click, see at a glance when one is unreachable, unauthorized or unhealthy. Its tooltip opens the ACM web UI, and a script or execution opens in ACM from its context menu (`ACM: Open ACM in Browser` picks any page).
- **AI agents.** Copilot gets the ACM scripting skill and the ACM MCP server for the active instance, registered automatically; other tools set up the same server from one copied prompt (see below).

## AI agents

The extension contributes:

- the [ACM Groovy scripting skill](https://github.com/wttech/acm/blob/main/tools/skills/acm-groovy-script/SKILL.md), so agents write scripts with the real ACM API, dry runs, abort checks and documentation;
- the [ACM MCP server](https://github.com/wttech/acm/tree/main/tools/mcp-server) for the active instance, so agents in agent mode can validate and run scripts, follow executions and read outputs.

The MCP server is bundled and runs on the editor's own Node.js; credentials come from VS Code secret storage, never from `mcp.json` or other files. Switching the instance in the status bar switches the server, and a `readonly` instance gets a read-only server. Turn it off with `acm.mcp.enabled`.

For tools that do not pick up MCP servers contributed by extensions (Claude Code, Cursor, Devin, …) run **ACM: Copy MCP Setup Prompt** and paste it into the agent. It describes what to register for the active instance, and the agent knows where its tool keeps MCP configuration. The prompt contains no secrets. It uses the standalone [`@wppes/acm-mcp-server`](https://www.npmjs.com/package/@wppes/acm-mcp-server) package, which needs Node.js 22 or later.

> Agents run code with your AEM permissions. Prefer `readonly` instances or a user with limited ACM permissions outside local development, and review tool calls before approving them.

## Configuration

Instances live in settings, typically the workspace's `.vscode/settings.json`, so a project can share them:

```json
"acm.instances": [
  { "name": "author", "url": "http://localhost:4502", "authMode": "basic", "user": "admin" },
  { "name": "dev", "url": "https://author-pXXXX-eYYYY.adobeaemcloud.com", "authMode": "bearer", "readonly": true }
]
```

Run `ACM: Select Instance` (or click the status bar) to switch, and `ACM: Set Credentials` to enter the secret:

| `authMode` | Secret | Typical use |
|---|---|---|
| `basic` | password of `user` | local AEM SDK, on-premise |
| `bearer` | access token, e.g. the local development token from the AEMaaCS Developer Console | AEM as a Cloud Service |
| `cookie` | value of the `login-token` cookie from a browser session | any instance you can log into |

Secrets are kept in VS Code secret storage and bound to the instance URL. When they expire, the status bar and error messages lead to `ACM: Set Credentials`.

Guardrails: running on a `readonly` instance is blocked, and running on any non-local instance asks for confirmation. In untrusted workspaces, instances are read from user settings only.

| Setting | Default | Description |
|---|---|---|
| `acm.instances` | local `author` | AEM instances with ACM installed. |
| `acm.activeInstance` | | Instance used by commands; the only instance when just one is configured. |
| `acm.validateOnSave` | `true` | Compile-check scripts declaring `doRun()` when saved. |
| `acm.mcp.enabled` | `true` | Offer the bundled ACM MCP server to agents. |
| `acm.http.timeout` | `30000` | Timeout of each HTTP request to AEM, in ms. |
| `acm.run.timeout` | `120000` | Wait for a run, in ms: the limit of a run without history, and how long the MCP server waits before returning the execution ID. Runs from the editor with history are followed until they finish. |
| `acm.run.pollInterval` | `1000` | How often a queued run is checked, in ms. |
| `acm.health.interval` | `60000` | How often the status bar checks the instance, in ms; `0` only on changes. |
| `acm.executions.limit` | `50` | Past executions shown in the Executions view. |

Timings apply to the bundled MCP server too.

## Requirements

- VS Code 1.101 or later (or a compatible editor).
- ACM installed on the instance, and a user with access to the ACM API and the console feature. See [ACM permissions](https://github.com/wttech/acm#tools-access-configuration).

## Contributing

To build, check and release the extension, see the [development guide](https://github.com/wttech/acm/blob/main/tools/vscode-extension/DEVELOPMENT.md) and the [roadmap](https://github.com/wttech/acm/blob/main/tools/vscode-extension/ROADMAP.md).

## License

[Apache License 2.0](https://github.com/wttech/acm/blob/main/LICENSE)
