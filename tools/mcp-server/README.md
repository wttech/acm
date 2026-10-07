# ACM MCP Server

[![npm](https://img.shields.io/npm/v/@wppes/acm-mcp-server)](https://www.npmjs.com/package/@wppes/acm-mcp-server)
[![MCP Server](https://github.com/wttech/acm/actions/workflows/release.mcp-server.yml/badge.svg)](https://github.com/wttech/acm/actions/workflows/release.mcp-server.yml)

A [Model Context Protocol](https://modelcontextprotocol.io) server that connects AI agents (Claude Code, Claude Desktop, VS Code, Cursor and other MCP clients) to [AEM Content Manager (ACM)](https://github.com/wttech/acm) running on Adobe Experience Manager, including AEM as a Cloud Service.

An agent can validate and run Groovy scripts on AEM through ACM, follow long-running executions, read execution history and console output, and browse the scripts stored on the instance. Every call runs as the user whose credentials you configure.

```
┌──────────────┐   stdio (MCP)   ┌────────────────┐   HTTPS    ┌─────────────────────┐
│  MCP client  │ ◄─────────────► │ acm-mcp-server │ ◄────────► │ AEM author + ACM    │
│ (Claude ...) │                 │  (your machine)│            │ /apps/acm/api/*.json│
└──────────────┘                 └────────────────┘            └─────────────────────┘
```

> [!WARNING]
> This server lets an AI agent run arbitrary Groovy code on AEM with your permissions. Read [Security](#security) before pointing it at a shared or production instance.

> [!TIP]
> Using VS Code? The [ACM extension](../vscode-extension/README.md) bundles this server and registers it for the active instance, with credentials from VS Code's secret storage. No setup below is needed.

## Requirements

- Node.js 22 or later.
- ACM installed on the AEM author instance. Running code without history (`history: false`) needs ACM 0.9.74 or later.
- A user with access to the ACM API (see [Permissions](#permissions)).

## Setup

The server is configured entirely through environment variables set in your MCP client. There are no config files.

### 1. Choose credentials

Set **one** of these options. The server detects which one you used.

| Variable | When to use | How to get it |
|---|---|---|
| `AEM_TOKEN` | AEM as a Cloud Service (recommended) | Cloud Manager → Developer Console of the environment → Integrations → Local token → "Get Local Development Token". Valid for 24 hours and acts as your user. |
| `AEM_COOKIE` | AEM as a Cloud Service, quick start | Log into AEM author in a browser and copy the value of the `login-token` cookie (DevTools → Application → Cookies). CSRF tokens are handled for you. |
| `AEM_COOKIE_FILE` | AEM as a Cloud Service, refreshed often | Path to a file holding the same `login-token` value. The file is re-read on every request, so you can refresh the credential without restarting the server. Takes precedence over `AEM_COOKIE`, which stays as a fallback. |
| `AEM_USER` + `AEM_PASSWORD` | Local AEM SDK | Usually `admin` / `admin`. |

The `login-token` cookie expires after about 12 hours. With `AEM_COOKIE` you must paste a new value and reconnect the agent each time. With `AEM_COOKIE_FILE` you only rewrite the file.

### 2. Register the server with your agent

Every MCP client needs the same things. Where they go and in which format depends on the client, so this guide does not list client-specific files:

| Setting | Value |
|---|---|
| Transport | stdio |
| Command | `npx` |
| Arguments | `-y`, `@wppes/acm-mcp-server` |
| Environment | `AEM_BASE_URL`, the credential from step 1, optional variables below |

The easiest way is to ask your agent, which knows its own configuration:

> Add the ACM MCP server to this tool's MCP configuration: transport stdio, command `npx`, arguments `-y @wppes/acm-mcp-server`, environment `AEM_BASE_URL=https://author-pXXXX-eYYYY.adobeaemcloud.com` and `AEM_TOKEN` for the credential, `ACM_READONLY=true`. Find in this tool's documentation where MCP servers are configured and in which format. Never write the credential into a file that may be committed and do not ask me to paste it here: reference an environment variable or the tool's secret mechanism, and tell me where to put the value. Then call `acm_health` to verify.

Keep the credential out of files that are committed. Most clients can reference an environment variable or ask for the value when the server starts.

### 3. Check the connection

Ask the agent to call `acm_health`. It reports the target instance, the auth mode, and the instance state as ACM reports it.

### Optional environment variables

| Variable | Description |
|---|---|
| `ACM_READONLY` | `true` disables the tools that run or abort code (`acm_run_code`, `acm_abort_execution`, `acm_describe_inputs`). Validation and read tools stay available. **Recommended for production**, but see [Security](#security): it is a guardrail, not a security boundary. |
| `AEM_AUTH` | Force the auth mode: `bearer`, `cookie` or `basic`. Normally detected from the credentials you set. |
| `ACM_RUN_TIMEOUT_MS` | How long `acm_run_code` waits before returning the execution ID for later polling. Default `120000`. |
| `ACM_POLL_INTERVAL_MS` | Queue polling interval. Default `1500`. |
| `AEM_HTTP_TIMEOUT_MS` | Timeout for each HTTP request. Default `30000`. |
| `AEM_UNAUTHORIZED_MESSAGE` | Replaces the hint returned on `401 Unauthorized`, for tools that manage the credentials themselves (e.g. the VS Code extension). |

## Tools

| Tool | Description |
|---|---|
| `acm_health` | Check connectivity, auth and the instance state ACM reports (`/apps/acm/api/state.json`), with a warning when ACM's health check finds the instance unhealthy. Call it first. |
| `acm_validate_code` | Compile-check Groovy without running it (`mode=parse`). Returns compile errors with line and column. Never recorded in execution history. |
| `acm_run_code` | Queue Groovy for execution, poll until it finishes or times out, and return the status and full console output. Supports `inputs` for scripts with `describeRun()`. With `history: false` it runs synchronously and is not recorded in history (see below). |
| `acm_get_execution` | Get the status, inputs, error and console output of an execution by ID, whether queued, running or archived. |
| `acm_abort_execution` | Abort a queued or running execution. Scripts that call `context.checkAborted()` stop cleanly. |
| `acm_list_executions` | List execution history, newest first, optionally only queued and running executions. |
| `acm_list_scripts` | List Groovy scripts stored under `/conf/acm/settings/script` by type (MANUAL, AUTOMATIC, …). |
| `acm_get_script` | Read the Groovy content of a stored script by ID or path. |
| `acm_describe_inputs` | Resolve the inputs a script declares in `describeRun()`: names, types and defaults. ACM runs `describeRun()` to do this, so it counts as running code. |
| `acm_get_output_file` | Download a named execution output: `console`, or an output created with `outputs.file(...)` / `outputs.text(...)`. |

Bare Groovy snippets are wrapped in the `canRun()`/`doRun()` structure automatically, so `println "hello"` is valid input for `acm_validate_code` and `acm_run_code`.

### Runs without history

Every queued run is stored in ACM execution history, so iterating on a script quickly fills it up. With `history: false`, `acm_run_code` calls `/apps/acm/api/execute-code.json` directly and the run is not recorded. Such a run:

- has no execution ID to poll or abort, and keeps no output files;
- is cut off on the client side after `waitMs`, while the script may keep running on AEM.

Use it for short read-only runs or dry runs while you develop a script. Run the final version, and anything that changes content, with the default `history: true` so the change stays auditable.

Running without history needs the `console/execute/nohistory` ACM feature (administrators only by default); without it the call fails with `403`. Such runs still appear in ACM's [audit log](https://github.com/wttech/acm#audit-log).

## Scripting guide

The server ships the [ACM Groovy scripting skill](../skills/acm-groovy-script/SKILL.md), so agents write valid, safe scripts without extra setup:

- **Instructions.** The skill's essentials (never invent API, dry runs, abort checks, validate before running) are sent to the client when it connects. Clients add them to the model's context.
- **Prompt.** `acm-groovy-script` returns the full guide.
- **Resources.** The guide and its references are available as `acm://skill/SKILL.md`, `acm://skill/references/api.md` (every ACM variable, class and method, generated from the ACM source), `acm://skill/references/scripts.md` and the script templates under `acm://skill/templates/`.

## Example

Ask the agent something like:

> Use ACM to count how many `wknd/components/teaser` components are under `/content/wknd/us/en`.

It calls `acm_health`, then validates and runs a script such as:

```groovy
boolean canRun() {
    return conditions.always()
}

void doRun() {
    def sql = "SELECT * FROM [nt:base] AS n WHERE ISDESCENDANTNODE(n, '/content/wknd/us/en') AND n.[sling:resourceType] = 'wknd/components/teaser'"
    def count = 0
    repo.queryRaw(sql).forEach { resource ->
        context.checkAborted()
        count++
    }
    out.info("Found ${count} instance(s)")
}
```

## Permissions

ACM checks access at three levels. The user behind the credentials needs `jcr:read` on each:

1. the API node under `/apps/acm/api`;
2. the feature node under `/apps/acm/feature`;
3. the script path under `/conf/acm/settings/script`.

By default only administrators have access. See [Tools Access Configuration](https://github.com/wttech/acm#tools-access-configuration). A `403` from any tool includes this hint. A `401` means the token or cookie has expired.

## Security

- **The agent acts as you.** Every script runs with the permissions of the configured user. A model can make mistakes, and content it reads (pages, scripts, execution output) can try to steer it. Use the least-privileged user that can do the job.
- **Use `ACM_READONLY=true` for production.** Health, validation, history and script-reading tools keep working; running, describing and aborting code are blocked. This only stops the agent from calling those tools. It is not a security boundary: validation still compiles the submitted Groovy on AEM, and Groovy compile-time transforms can run code. The real control is the AEM permissions of the configured user.
- **Runs are traceable.** Queued runs are kept in ACM history; runs that leave no history (without history, or not queued by `canRun()`) are written to ACM's [audit log](https://github.com/wttech/acm#audit-log) with the user and a checksum of the code. Grant `console/execute/nohistory` only to users who need it.
- **Grant agents only what they need.** A user with the `script/execute` feature but without `console/execute` can only run the stored scripts as they are.
- **Review scripts before they run** against shared instances. Don't auto-approve `acm_run_code` in your MCP client for those instances.
- **Prefer dry runs for destructive changes.** Use ACM's `repo.dryRun(...)` pattern; the bundled scripting guide steers the model towards it.
- **Credentials stay local.** The server runs on your machine and only talks to `AEM_BASE_URL`. Keep tokens in your MCP client's secret storage or environment, not in files you commit.

## Contributing

To build, test and release the server, see the [development guide](https://github.com/wttech/acm/blob/main/tools/mcp-server/DEVELOPMENT.md).

## License

Part of AEM Content Manager, licensed under the [Apache License, Version 2.0](https://github.com/wttech/acm/blob/main/LICENSE).
