# ACM MCP Server

[![npm](https://img.shields.io/npm/v/@wppes/acm-mcp-server)](https://www.npmjs.com/package/@wppes/acm-mcp-server)
[![MCP Server](https://github.com/wttech/acm/actions/workflows/mcp-server.yml/badge.svg)](https://github.com/wttech/acm/actions/workflows/mcp-server.yml)

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

The examples use a bearer token. Use the variables from step 1 to pick a different option.

**Claude Code**

```bash
claude mcp add acm \
  --env AEM_BASE_URL=https://author-pXXXX-eYYYY.adobeaemcloud.com \
  --env AEM_TOKEN=eyJhbGciOi... \
  -- npx -y @wppes/acm-mcp-server
```

**Claude Desktop** (`claude_desktop_config.json`) and **Cursor** (`~/.cursor/mcp.json`)

```json
{
  "mcpServers": {
    "acm": {
      "command": "npx",
      "args": ["-y", "@wppes/acm-mcp-server"],
      "env": {
        "AEM_BASE_URL": "https://author-pXXXX-eYYYY.adobeaemcloud.com",
        "AEM_TOKEN": "eyJhbGciOi..."
      }
    }
  }
}
```

**VS Code** (`.vscode/mcp.json`). VS Code prompts for the token, so it is not stored in the file:

```json
{
  "inputs": [
    { "type": "promptString", "id": "aem-token", "description": "AEM bearer token", "password": true }
  ],
  "servers": {
    "acm": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@wppes/acm-mcp-server"],
      "env": {
        "AEM_BASE_URL": "https://author-pXXXX-eYYYY.adobeaemcloud.com",
        "AEM_TOKEN": "${input:aem-token}"
      }
    }
  }
}
```

### 3. Check the connection

Ask the agent to call `acm_health`. It reports the target instance, the auth mode, and the ACM instance state.

### Optional environment variables

| Variable | Description |
|---|---|
| `ACM_READONLY` | `true` disables the tools that run or abort code (`acm_run_code`, `acm_abort_execution`, `acm_describe_inputs`). Validation and read tools stay available. **Recommended for production**, but see [Security](#security): it is a guardrail, not a security boundary. |
| `AEM_AUTH` | Force the auth mode: `bearer`, `cookie` or `basic`. Normally detected from the credentials you set. |
| `ACM_RUN_TIMEOUT_MS` | How long `acm_run_code` waits before returning the execution ID for later polling. Default `120000`. |
| `ACM_POLL_INTERVAL_MS` | Queue polling interval. Default `1500`. |
| `AEM_HTTP_TIMEOUT_MS` | Timeout for each HTTP request. Default `30000`. |

## Tools

| Tool | Description |
|---|---|
| `acm_health` | Check connectivity, auth and ACM instance state (`/apps/acm/api/state.json`). Call it first. |
| `acm_validate_code` | Compile-check Groovy without running it (`mode=parse`). Returns compile errors with line and column. Never recorded in execution history. |
| `acm_run_code` | Queue Groovy for execution, poll until it finishes or times out, and return the status and full console output. Supports `inputs` for scripts with `describeRun()`. With `history: false` it runs synchronously and is not recorded in history (see below). |
| `acm_get_execution` | Get the status, error and console output of an execution by ID, whether queued, running or archived. |
| `acm_abort_execution` | Abort a queued or running execution. Scripts that call `context.checkAborted()` stop cleanly. |
| `acm_list_executions` | List execution history, newest first, optionally only queued and running executions. |
| `acm_list_scripts` | List Groovy scripts stored under `/conf/acm/settings/script` by type (MANUAL, AUTOMATIC, …). |
| `acm_get_script` | Read the Groovy content of a stored script by ID or path. |
| `acm_describe_inputs` | Resolve the inputs a script declares in `describeRun()`: names, types and defaults. ACM runs `describeRun()` to do this, so it counts as running code. |
| `acm_get_output_file` | Download a named execution output: `console`, or an output created with `outputs.file(...)` / `outputs.text(...)`. |

The server also provides an `acm-scripting-guide` MCP prompt. It is a short summary of ACM Groovy conventions (`canRun()`/`doRun()`, `repo`, `acl`, `out`, `conditions`, `context.checkAborted()`, dry runs) that helps the model write valid scripts.

Bare Groovy snippets are wrapped in the `canRun()`/`doRun()` structure automatically, so `println "hello"` is valid input for `acm_validate_code` and `acm_run_code`.

### Runs without history

Every queued run is stored in ACM execution history, so iterating on a script quickly fills it up. With `history: false`, `acm_run_code` calls `/apps/acm/api/execute-code.json` directly and the run is not recorded. Such a run:

- has no execution ID to poll or abort, and keeps no output files;
- is cut off on the client side after `waitMs`, while the script may keep running on AEM.

Use it for short read-only runs or dry runs while you develop a script. Run the final version, and anything that changes content, with the default `history: true` so the change stays auditable.

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
- **Review scripts before they run** against shared instances. Don't auto-approve `acm_run_code` in your MCP client for those instances.
- **Prefer dry runs for destructive changes.** Use ACM's `repo.dryRun(...)` pattern; the bundled scripting guide steers the model towards it.
- **Credentials stay local.** The server runs on your machine and only talks to `AEM_BASE_URL`. Keep tokens in your MCP client's secret storage or environment, not in files you commit.

## Development

```bash
cd tools/mcp-server
npm install
npm test          # builds, then runs the end-to-end tests
```

`npm test` starts the built server over stdio against a mock ACM backend and calls every tool:

- `test/mock-acm.mjs` emulates the ACM servlets under `/apps/acm/api/*`: the response envelope, all three auth modes, Granite CSRF, and a QUEUED → RUNNING → SUCCEEDED lifecycle, so the polling loop is really exercised.
- `test/run-tests.mjs` drives the server with the MCP SDK client: handshake, tool listing and every tool call.

To try the server against a real instance, run `test/smoke.mjs`. It calls `acm_health` and runs one harmless `println`. It reads connection settings from a `.env` file, which is git-ignored. The server itself never reads `.env`.

```bash
cp .env.example .env     # set AEM_BASE_URL and ONE auth option
npm run smoke
```

To run a local build from an MCP client, point it at `node /path/to/acm/tools/mcp-server/dist/index.js` instead of `npx`.

## Releasing

The server is versioned and released separately from ACM, with tags prefixed `mcp-server-v`.

1. Bump the version in `package.json` and `package-lock.json`:

    ```shell
    npm version <version> --no-git-tag-version
    ```

2. Set the same version in both `version` fields of `server.json`, then merge to `main`.
3. Tag the merge commit and push the tag:

    ```shell
    git tag mcp-server-v<version>
    git push origin mcp-server-v<version>
    ```

The [MCP Server workflow](https://github.com/wttech/acm/blob/main/.github/workflows/mcp-server.yml) runs the tests and checks that the tag matches all three files. It then publishes the package to npm with provenance and publishes `server.json` to the [MCP Registry](https://registry.modelcontextprotocol.io). If only the registry step fails, re-run the job. The npm step skips versions that are already published.

### One-time setup

npm trusted publishing can only be configured for a package that already exists, so the first version is published by hand:

1. Log in with an account that can publish to the `@wppes` scope, then run `npm ci && npm publish` in `tools/mcp-server/`.
2. On npmjs.com, open the package's settings and add a trusted publisher: GitHub Actions, organization `wttech`, repository `acm`, workflow `mcp-server.yml`.
3. Optionally, set publishing access to require two-factor authentication and disallow tokens, so only the workflow can publish.
4. Push the `mcp-server-v<version>` tag for that version. The workflow skips the npm step and publishes to the MCP Registry.

## License

Part of AEM Content Manager, licensed under the [Apache License, Version 2.0](https://github.com/wttech/acm/blob/main/LICENSE).
