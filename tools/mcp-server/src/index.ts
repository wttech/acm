#!/usr/bin/env node
/**
 * ============================================================================
 *  ACM MCP Server — Model Context Protocol server for AEM Content Manager
 *  (https://github.com/wttech/acm)
 * ============================================================================
 *
 *  Lets an MCP client (Claude Code, Claude Desktop, VS Code, etc.) talk to
 *  one or more AEM instances running ACM (incl. AEM as a Cloud Service):
 *  validate Groovy code, execute it (queued with polling, or synchronously
 *  without recording history), inspect execution history, read console
 *  output and generated output files, browse and describe stored scripts.
 *
 *  ----------------------------------------------------------------------------
 *  RUN
 *  ----------------------------------------------------------------------------
 *    npx -y @wppes/acm-mcp-server
 *
 *  From source:  npm install && npm run build && node dist/index.js
 *
 *  ----------------------------------------------------------------------------
 *  CONFIGURATION
 *  ----------------------------------------------------------------------------
 *    ACM_CONFIG_FILE  Path to the config document (JSON), or
 *    ACM_CONFIG       the same document inline (takes precedence). At least
 *                     one is required; the VS Code extension passes ACM_CONFIG.
 *
 *  The document uses the same keys as the extension settings, including the
 *  "acm." prefix — a block copied from VS Code settings.json is valid here:
 *
 *    {
 *      "acm.instances": [
 *        { "name": "local-author", "url": "http://localhost:4502",
 *          "authMode": "basic", "user": "admin" },
 *        { "name": "dev", "url": "https://author-p1-e2.adobeaemcloud.com",
 *          "authMode": "bearer", "secret": { "env": "ACM_DEV_TOKEN" },
 *          "readonly": true },
 *        { "name": "prod", "url": "https://prod.example.com",
 *          "authMode": "cookie", "secret": { "file": "./prod.cookie" },
 *          "enabled": false }
 *      ],
 *      "acm.activeInstance": "local-author",
 *      "acm.http.timeout": 30000,
 *      "acm.run.timeout": 120000,
 *      "acm.run.pollInterval": 1000
 *    }
 *
 *    secret      The token, cookie (value or name=value) or password: a
 *                literal, { "env": "NAME" } or { "file": "path" }. A file is
 *                resolved relative to the config file and re-read on every
 *                request, so refreshing a cookie on disk needs no restart.
 *                Keep the file readable only by you (chmod 600). Basic auth
 *                with user admin on a local URL defaults to password admin.
 *    readonly    Blocks the tools that run or abort code on this instance.
 *                A guardrail, not a security boundary.
 *    enabled     false hides the instance from all tools (default true).
 *
 *    acm.activeInstance picks the default instance: with it set, or with a
 *    single enabled instance, tools need no "instance" argument; with
 *    several enabled instances and no active one, every call must name it.
 *
 *  ----------------------------------------------------------------------------
 *  MCP CLIENT CONFIG EXAMPLE
 *  ----------------------------------------------------------------------------
 *    {
 *      "mcpServers": {
 *        "acm": {
 *          "command": "npx",
 *          "args": ["-y", "@wppes/acm-mcp-server"],
 *          "env": { "ACM_CONFIG_FILE": "/path/to/acm-config.json" }
 *        }
 *      }
 *    }
 * ============================================================================
 */

import { readFileSync, statSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, isAbsolute, resolve } from "node:path";

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

import {
  ACM_API,
  type AcmAuth,
  AcmClient,
  type AcmInstance,
  activeInstanceProblem,
  CONSOLE_CODE_ID,
  MCP_ENV,
  type Execution,
  type ExecutionListOutput,
  type QueueOutput,
  defaultSecret,
  describeUnhealthy,
  enabledInstances,
  fetchConsoleOutput,
  fetchExecutionById,
  isFailed,
  isPending,
  normalizeCookie,
  normalizeGroovy,
  OUTPUT_NAMES,
  parseConfig,
  resolveActive,
  resolveInstance,
  type AcmConfig,
  SCRIPT_TEMPLATES,
  SKILL_DOCUMENTS,
  SKILL_ESSENTIALS,
  SKILL_NAME,
  summarizeExecution,
  type HealthStatus,
  waitForExecution,
} from "@acm/shared";

/* ============================================================================
 * Configuration: the config document, instance secrets, per-instance clients
 * ========================================================================== */

/** The parsed config plus where it came from; `baseDir` resolves relative secret files. */
interface LoadedConfig {
  config: AcmConfig;
  baseDir: string;
  source?: string;
}

function fatal(message: string): never {
  console.error(`[acm-mcp] ${message}`);
  process.exit(1);
}

const filePath = (p: string, baseDir: string): string => {
  const expanded = p.startsWith("~/") || p === "~" ? homedir() + p.slice(1) : p;
  return isAbsolute(expanded) ? expanded : resolve(baseDir, expanded);
};

function loadConfig(): LoadedConfig {
  const inline = process.env[MCP_ENV.config]?.trim();
  const file = process.env[MCP_ENV.configFile]?.trim();
  if (!inline && !file) {
    fatal(
      `${MCP_ENV.configFile} or ${MCP_ENV.config} is required. ` +
        `Point one of them at an ACM config document; see the README for its format.`
    );
  }
  let doc: unknown;
  let baseDir = process.cwd();
  let source: string | undefined;
  try {
    if (inline) {
      doc = JSON.parse(inline);
    } else {
      source = filePath(file!, process.cwd());
      baseDir = dirname(source);
      doc = JSON.parse(readFileSync(source, "utf8"));
    }
  } catch (e) {
    fatal(`Cannot read the ACM config${source ? ` from ${source}` : ` in ${MCP_ENV.config}`}: ${e instanceof Error ? e.message : e}`);
  }
  try {
    return { config: parseConfig(doc), baseDir, source };
  } catch (e) {
    fatal(`${e instanceof Error ? e.message : e}`);
  }
}

const { config, baseDir, source: configPath } = loadConfig();

// A config file holding literal secrets should not be world-readable.
if (configPath && process.platform !== "win32") {
  const hasLiterals = config.instances.some((instance) => typeof instance.secret === "string");
  try {
    if (hasLiterals && (statSync(configPath).mode & 0o077) !== 0) {
      console.error(
        `[acm-mcp] ${configPath} holds literal secrets and is readable by group or others. ` +
          `Restrict it (chmod 600) or move secrets to { "env" } / { "file" } references.`
      );
    }
  } catch {
    // ignore — the file read itself already succeeded
  }
}

const activeProblem = activeInstanceProblem(config.instances, config.activeInstance);
if (activeProblem) {
  console.error(`[acm-mcp] ${activeProblem} Tools use the only enabled instance, or need "instance" when several are enabled.`);
}

/** The instance's secret: a literal, an env var, or a file re-read on every call. */
function secretResolver(instance: AcmInstance): () => string | undefined {
  const ref = instance.secret;
  if (typeof ref === "string") return () => ref.trim() || undefined;
  if (ref && "env" in ref) return () => process.env[ref.env]?.trim() || undefined;
  if (ref && "file" in ref) {
    const path = filePath(ref.file, baseDir);
    return () => {
      try {
        return readFileSync(path, "utf8").trim() || undefined;
      } catch {
        return undefined;
      }
    };
  }
  return () => defaultSecret(instance);
}

function missingSecretMessage(instance: AcmInstance): string {
  const kind =
    instance.authMode === "bearer"
      ? "bearer token"
      : instance.authMode === "cookie"
        ? "login-token cookie"
        : `password of ${instance.user ?? "the user"}`;
  const ref = instance.secret;
  const where =
    typeof ref === "string"
      ? 'the "secret" value'
      : ref && "env" in ref
        ? `the ${ref.env} environment variable`
        : ref && "file" in ref
          ? `the file ${ref.file} (re-read per request; it may be missing or empty)`
          : '"secret" in the config (literal, { "env" } or { "file" })';
  return `No credentials for ACM instance "${instance.name}": no ${kind} in ${where}.`;
}

function createAuth(instance: AcmInstance, secret: () => string | undefined): AcmAuth {
  switch (instance.authMode) {
    case "bearer":
      return { mode: "bearer", token: secret };
    case "cookie":
      return { mode: "cookie", cookie: () => normalizeCookie(secret()) };
    case "basic":
      return { mode: "basic", user: instance.user ?? "", password: secret };
  }
}

const UNAUTHORIZED_MESSAGES: Record<AcmInstance["authMode"], string> = {
  bearer:
    "401 Unauthorized — the bearer token is invalid or expired. AEMaaCS local development tokens expire after 24h: fetch a fresh one from Cloud Manager → Developer Console → Integrations → Local token, and update the instance's secret.",
  cookie:
    "401 Unauthorized — the login-token cookie is invalid or expired. Log into AEM author in the browser and update the instance's secret.",
  basic: "401 Unauthorized — check the instance's user and password.",
};

const unauthorizedTemplate = process.env[MCP_ENV.unauthorizedMessage];

interface InstanceTarget {
  instance: AcmInstance;
  client: AcmClient;
}

const targets = new Map<string, InstanceTarget>();

function clientFor(instance: AcmInstance): AcmClient {
  let target = targets.get(instance.name);
  if (!target) {
    target = {
      instance,
      client: new AcmClient({
        baseUrl: instance.url,
        auth: createAuth(instance, secretResolver(instance)),
        timeoutMs: config.httpTimeoutMs,
        messages: {
          unauthorized: (unauthorizedTemplate ?? UNAUTHORIZED_MESSAGES[instance.authMode]).replaceAll(
            "{instance}",
            instance.name
          ),
          missingSecret: missingSecretMessage(instance),
        },
      }),
    };
    targets.set(instance.name, target);
  }
  return target.client;
}

/** The instance a call targets, or an error when the name is wrong or none is active. */
function pickTarget(requested?: string): InstanceTarget | { error: ReturnType<typeof errorResult> } {
  try {
    const instance = resolveInstance(config.instances, config.activeInstance, requested);
    return { instance, client: clientFor(instance) };
  } catch (e) {
    return { error: errorResult(e) };
  }
}

const instanceInfo = (instance: AcmInstance, client?: AcmClient) =>
  `Instance: ${instance.name} (${instance.url})${client ? ` | Auth: ${client.authDescription}` : ""}${instance.readonly ? " | READ-ONLY MODE" : ""}`;

const instanceResult = (instance: AcmInstance, text: string, isError = false) =>
  textResult(`${instanceInfo(instance)}\n\n${text}`, isError);

const readonlyBlock = (instance: AcmInstance, action: string) =>
  instanceResult(
    instance,
    `BLOCKED: this instance is read-only — ${action} is disabled. Only validation and read tools are available.`,
    true
  );

/* ============================================================================
 * MCP server & tools
 * ========================================================================== */

const { version } = JSON.parse(readFileSync(new URL("../package.json", import.meta.url), "utf8")) as {
  version: string;
};

const skillUri = (path: string) => `acm://skill/${path}`;

const server = new McpServer(
  {
    name: "acm-mcp-server",
    title: "AEM Content Manager (ACM)",
    description: "Validate and run Groovy scripts on Adobe Experience Manager through AEM Content Manager (ACM).",
    websiteUrl: "https://github.com/wttech/acm/tree/main/tools/mcp-server",
    version,
  },
  {
    instructions: [
      "Tools for running Groovy code on AEM through AEM Content Manager (ACM). Follow these rules when writing ACM code:",
      SKILL_ESSENTIALS,
      `The full guide is the '${SKILL_NAME}' prompt. Its references, including the generated API reference, are resources: ${SKILL_DOCUMENTS.map((d) => skillUri(d.path)).join(", ")}.`,
    ].join("\n\n"),
  }
);

function textResult(text: string, isError = false) {
  return { content: [{ type: "text" as const, text }], isError };
}

function errorResult(e: unknown) {
  const msg = e instanceof Error ? e.message : String(e);
  return textResult(`ERROR: ${msg}`, true);
}

const instanceParam = z
  .string()
  .optional()
  .describe("ACM instance name; defaults to the active instance. See acm_list_instances.");

/* --------------------------------------------------------- list instances -- */
server.registerTool(
  "acm_list_instances",
  {
    title: "List ACM instances",
    description:
      "List the enabled AEM instances this server can reach: name, URL, auth mode, read-only flag, which is active and whether credentials are configured. Other tools accept an instance name as the optional \"instance\" argument.",
    inputSchema: {},
  },
  () => {
    const active = resolveActive(config.instances, config.activeInstance);
    const lines = enabledInstances(config.instances).map((instance) => {
      const hasSecret = !!secretResolver(instance)();
      return (
        `- ${instance.name}${instance.name === active?.name ? " (active)" : ""} | ${instance.url}` +
        ` | ${instance.authMode}${instance.readonly ? " | read-only" : ""}` +
        ` | credentials: ${hasSecret ? "configured" : "MISSING"}`
      );
    });
    return textResult(`${activeProblem ? `WARNING: ${activeProblem}\n\n` : ""}Instances (${lines.length}):\n${lines.join("\n")}`);
  }
);

/* ---------------------------------------------------------------- health -- */
server.registerTool(
  "acm_health",
  {
    title: "ACM health & state",
    description:
      `Check connectivity, authentication and the state ACM reports for the instance (health check result, queued executions, instance info). Call this first to verify the setup; an unhealthy instance is flagged with a warning because running scripts on it may be unsafe. Returns the raw state from ${ACM_API.state}. Without "instance", checks every enabled instance.`,
    inputSchema: { instance: instanceParam },
  },
  async ({ instance: name }) => {
    if (name) {
      const target = pickTarget(name);
      if ("error" in target) return target.error;
      const section = await healthText(target.instance, target.client);
      return textResult(section.text, section.failed);
    }
    const enabled = enabledInstances(config.instances);
    if (enabled.length === 0) {
      return textResult("ERROR: No enabled ACM instances: every instance in the config is disabled.", true);
    }
    const sections: HealthSection[] = [];
    for (const instance of enabled) {
      sections.push(await healthText(instance, clientFor(instance)));
    }
    return textResult(
      sections.map((section) => section.text).join("\n\n---\n\n"),
      sections.every((section) => section.failed)
    );
  }
);

/** One instance's health result: the text shown to the caller and whether the check failed. */
interface HealthSection {
  text: string;
  failed: boolean;
}

async function healthText(instance: AcmInstance, client: AcmClient): Promise<HealthSection> {
  const header = instanceInfo(instance, client);
  try {
    const res = await client.request<{ healthStatus?: HealthStatus }>("GET", ACM_API.state);
    const warning = describeUnhealthy(res.data?.healthStatus);
    return {
      text: `${header}\n\n${warning ? `WARNING: ${warning}\n\n` : ""}ACM state:\n${JSON.stringify(res.data, null, 2)}`,
      failed: false,
    };
  } catch (e) {
    return { text: `${header}\n\nERROR: ${e instanceof Error ? e.message : e}`, failed: true };
  }
}

/* -------------------------------------------------------------- validate -- */
server.registerTool(
  "acm_validate_code",
  {
    title: "Validate Groovy code (compile only)",
    description:
      `Compile-check Groovy code with ACM without executing it (mode=parse on ${ACM_API.executeCode}). Returns compile errors with line/column if any. ALWAYS validate code before running it. Never recorded in ACM execution history. Code is auto-wrapped in canRun()/doRun() if not already structured that way.`,
    inputSchema: {
      code: z.string().describe("Groovy code. Either a full ACM script (canRun/doRun) or a bare snippet."),
      instance: instanceParam,
    },
  },
  async ({ code, instance: name }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    try {
      const content = normalizeGroovy(code);
      const res = await client.request<Execution>("POST", ACM_API.executeCode, {
        mode: "parse",
        code: { id: CONSOLE_CODE_ID, content },
      });
      const execution = res.data;
      if (execution.error) {
        const m = execution.error.match(/@ line (\d+), column (\d+)/);
        const loc = m ? ` (line ${m[1]}, column ${m[2]} of the normalized script)` : "";
        return instanceResult(
          instance,
          `COMPILE ERROR${loc}:\n${execution.error}\n\nNormalized script that was checked:\n${content}`,
          true
        );
      }
      return instanceResult(instance, "Code compiles successfully. ✔");
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* ------------------------------------------------------------------- run -- */
server.registerTool(
  "acm_run_code",
  {
    title: "Run Groovy code on AEM via ACM",
    description:
      `Run Groovy code on the AEM instance on behalf of the authenticated user and return the final status with full console output. By default (history=true) the code is queued (POST ${ACM_API.queueCode}) and polled until it finishes; the execution is recorded in ACM history, and if it is still running when the timeout elapses the executionId is returned — use acm_get_execution to keep checking. With history=false the code runs synchronously (POST ${ACM_API.executeCode}) and is NOT recorded in ACM history: use it while developing or debugging a script, for short read-only or dry-run runs, so repeated attempts do not flood the history. A history=false run has no trackable executionId, cannot be aborted, keeps no output files, and is cut off client-side after waitMs while the script may keep running on AEM. Run anything that changes content with history=true, so the change stays auditable. Prefer dry-run patterns (repo.dryRun) for destructive operations. Code is auto-wrapped in canRun()/doRun() if needed.`,
    inputSchema: {
      code: z.string().describe("Groovy code to execute (full ACM script or bare snippet)."),
      instance: instanceParam,
      inputs: z
        .record(z.string(), z.unknown())
        .optional()
        .describe("Optional input values for scripts using describeRun()/inputs, e.g. {\"dryRun\": true}."),
      waitMs: z
        .number()
        .int()
        .positive()
        .optional()
        .describe(`Max time to wait for completion in ms (default ${config.runTimeoutMs}).`),
      history: z
        .boolean()
        .optional()
        .describe(
          "Record the run in ACM execution history (default true). Set false for development/debug runs of short read-only or dry-run scripts; see the tool description for the trade-offs."
        ),
    },
  },
  async ({ code, instance: name, inputs, waitMs, history }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    if (instance.readonly) {
      return readonlyBlock(instance, "code execution");
    }
    const content = normalizeGroovy(code);
    // InputValues on the server side is a flat HashMap<String, Object>
    const inputValues = inputs && Object.keys(inputs).length > 0 ? inputs : undefined;
    if (history === false) return runWithoutHistory(target, content, inputValues, waitMs ?? config.runTimeoutMs);
    try {
      const res = await client.request<QueueOutput>("POST", ACM_API.queueCode, {
        code: { id: CONSOLE_CODE_ID, content },
        inputs: inputValues,
      });

      let execution = res.data?.executions?.[0];
      if (!execution) {
        return instanceResult(instance, `Queued, but no execution returned. Server message: ${res.message}`, true);
      }
      if (execution.status?.toUpperCase() === "SKIPPED") {
        return instanceResult(
          instance,
          `Execution SKIPPED — canRun() returned false (or a CHECK-phase condition prevented the run).\n\n${summarizeExecution(execution)}`
        );
      }

      const wait = waitMs ?? config.runTimeoutMs;
      execution = await waitForExecution(client, execution, { intervalMs: config.runPollIntervalMs, timeoutMs: wait });

      if (isPending(execution.status)) {
        return instanceResult(
          instance,
          `Execution still ${execution.status} after ${wait} ms.\nExecution ID: ${execution.id}\nUse acm_get_execution with this ID to check progress, or acm_abort_execution to stop it.`
        );
      }

      const consoleOut = await fetchConsoleOutput(client, execution.id);
      return instanceResult(instance, summarizeExecution(execution, consoleOut), isFailed(execution.status));
    } catch (e) {
      return errorResult(e);
    }
  }
);

/**
 * Run code synchronously with history disabled. ACM's execute-code servlet
 * honours a per-request `history` flag; the queue servlet does not.
 */
async function runWithoutHistory(
  { instance, client }: InstanceTarget,
  content: string,
  inputs: Record<string, unknown> | undefined,
  timeoutMs: number
) {
  try {
    const res = await client.request<Execution>(
      "POST",
      ACM_API.executeCode,
      { mode: "RUN", history: false, code: { id: CONSOLE_CODE_ID, content }, inputs },
      true,
      timeoutMs
    );
    const execution = res.data;
    const note = "(Not recorded in ACM history.)";
    if (execution.status?.toUpperCase() === "SKIPPED") {
      return instanceResult(
        instance,
        `Execution SKIPPED — canRun() returned false (or a CHECK-phase condition prevented the run). ${note}\n\n${summarizeExecution(execution)}`
      );
    }
    return instanceResult(instance, `${summarizeExecution(execution)}\n\n${note}`, isFailed(execution.status));
  } catch (e) {
    if (e instanceof Error && e.name === "AbortError") {
      return instanceResult(
        instance,
        `No response after ${timeoutMs} ms. A run without history cannot be tracked or aborted, and the script may still be running on AEM. Rerun it with history=true (queued, trackable, abortable) if it needs more time.`,
        true
      );
    }
    return errorResult(e);
  }
}

/* --------------------------------------------------------- get execution -- */
server.registerTool(
  "acm_get_execution",
  {
    title: "Get execution status & output",
    description:
      "Fetch the current status, error and console output of an execution by ID — whether it is still queued/running or already archived in history. Use after acm_run_code timed out, or to inspect any past execution.",
    inputSchema: {
      executionId: z.string().describe("The execution ID, e.g. as returned by acm_run_code."),
      instance: instanceParam,
    },
  },
  async ({ executionId, instance: name }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    try {
      const execution = await fetchExecutionById(client, executionId);
      if (!execution) return instanceResult(instance, `Execution '${executionId}' not found (queue or history).`, true);
      const consoleOut = isPending(execution.status) ? null : await fetchConsoleOutput(client, executionId);
      return instanceResult(instance, summarizeExecution(execution, consoleOut));
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* ------------------------------------------------------------------ abort -- */
server.registerTool(
  "acm_abort_execution",
  {
    title: "Abort a running execution",
    description:
      `Abort a queued or running execution (DELETE ${ACM_API.queueCode}?executionId=...). Scripts honoring context.checkAborted() will stop gracefully.`,
    inputSchema: {
      executionId: z.string().describe("ID of the execution to abort."),
      instance: instanceParam,
    },
  },
  async ({ executionId, instance: name }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    if (instance.readonly) {
      return readonlyBlock(instance, "aborting executions");
    }
    try {
      const res = await client.request<unknown>(
        "DELETE",
        `${ACM_API.queueCode}?executionId=${encodeURIComponent(executionId)}`
      );
      return instanceResult(instance, `Abort requested. Server message: ${res.message}`);
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* -------------------------------------------------------- list executions -- */
server.registerTool(
  "acm_list_executions",
  {
    title: "List execution history",
    description:
      `List past executions from ACM history (${ACM_API.execution}), newest first, in summary format. Optionally filter to currently queued executions only.`,
    inputSchema: {
      instance: instanceParam,
      limit: z.number().int().positive().max(200).optional().describe("Max results (default 20)."),
      offset: z.number().int().nonnegative().optional().describe("Pagination offset (default 0)."),
      queuedOnly: z.boolean().optional().describe("If true, list only currently queued/running executions."),
    },
  },
  async ({ instance: name, limit, offset, queuedOnly }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    try {
      const params = new URLSearchParams({ format: "summary" });
      params.set("limit", String(limit ?? 20));
      if (offset) params.set("offset", String(offset));
      if (queuedOnly) params.set("queued", "true");
      const res = await client.request<ExecutionListOutput>(
        "GET",
        `${ACM_API.execution}?${params.toString()}`
      );
      const list = res.data?.list || [];
      if (list.length === 0) return instanceResult(instance, "No executions found.");
      const rows = list.map(
        (e) =>
          `${e.id} | ${e.status} | ${e.executable?.id ?? e.executableId ?? "?"} | user=${e.userId ?? "?"} | start=${e.startDate ?? "?"} | ${e.duration ?? "?"} ms${e.error ? " | ERROR" : ""}`
      );
      return instanceResult(instance, `Executions (${list.length}):\n` + rows.join("\n"));
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* ----------------------------------------------------------- list scripts -- */
server.registerTool(
  "acm_list_scripts",
  {
    title: "List stored ACM scripts",
    description:
      `List Groovy scripts stored on the instance under /conf/acm/settings/script (GET ${ACM_API.script}?type=...).`,
    inputSchema: {
      instance: instanceParam,
      type: z
        .enum(["MANUAL", "AUTOMATIC", "ENABLED", "DISABLED", "EXTENSION", "MOCK"])
        .optional()
        .describe("Script type/tab to list (default MANUAL)."),
    },
  },
  async ({ instance: name, type }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    try {
      const res = await client.request<{ list?: Array<{ id: string; path?: string; [k: string]: unknown }> }>(
        "GET",
        `${ACM_API.script}?type=${encodeURIComponent(type ?? "MANUAL")}`
      );
      const list = res.data?.list || [];
      if (list.length === 0) return instanceResult(instance, `No ${type ?? "MANUAL"} scripts found.`);
      return instanceResult(
        instance,
        `Scripts (${list.length}):\n` + list.map((s) => `- ${s.id}${s.path && s.path !== s.id ? ` (${s.path})` : ""}`).join("\n")
      );
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* ------------------------------------------------------------- get script -- */
server.registerTool(
  "acm_get_script",
  {
    title: "Read a stored ACM script",
    description:
      "Fetch a stored script's Groovy content by ID/path (e.g. /conf/acm/settings/script/manual/example/foo.groovy).",
    inputSchema: {
      id: z.string().describe("Script ID — its repository path under /conf/acm/settings/script."),
      instance: instanceParam,
    },
  },
  async ({ id, instance: name }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    try {
      const res = await client.request<{ list?: Array<{ id: string; content?: string }> }>(
        "GET",
        `${ACM_API.script}?id=${encodeURIComponent(id)}`
      );
      const script = res.data?.list?.[0];
      if (!script) return instanceResult(instance, `Script '${id}' not found.`, true);
      return instanceResult(instance, `Script: ${script.id}\n\n${script.content ?? "(no content)"}`);
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* -------------------------------------------------------- describe inputs -- */
server.registerTool(
  "acm_describe_inputs",
  {
    title: "Describe code inputs",
    description:
      `Resolve the inputs a script declares in describeRun() (POST ${ACM_API.describeCode}). Use this before acm_run_code for scripts with inputs, to learn names, types and defaults.`,
    inputSchema: {
      code: z.string().describe("Groovy code (full ACM script) whose inputs should be described."),
      instance: instanceParam,
    },
  },
  async ({ code, instance: name }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    // ACM runs the script's describeRun() to resolve inputs, so this executes code.
    if (instance.readonly) {
      return readonlyBlock(instance, "describing inputs");
    }
    try {
      const res = await client.request<unknown>("POST", ACM_API.describeCode, {
        code: { id: CONSOLE_CODE_ID, content: code },
      });
      return instanceResult(instance, `Description:\n${JSON.stringify(res.data, null, 2)}`);
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* ------------------------------------------------------------ output file -- */
server.registerTool(
  "acm_get_output_file",
  {
    title: "Download an execution output",
    description:
      `Fetch a named output of an execution from ${ACM_API.executionOutput} — 'console' for console text, or the name given to outputs.file(...)/outputs.text(...) in the script (e.g. 'report'). Returns text content; binary outputs are returned base64-encoded.`,
    inputSchema: {
      executionId: z.string().describe("Execution ID."),
      name: z.string().describe("Output name: 'console', 'archive', or a script-defined output name."),
      instance: instanceParam,
    },
  },
  async ({ executionId, name: outputName, instance: name }) => {
    const target = pickTarget(name);
    if ("error" in target) return target.error;
    const { instance, client } = target;
    const resolvedName =
      outputName === "console" ? OUTPUT_NAMES.console : outputName === "archive" ? OUTPUT_NAMES.archive : outputName;
    try {
      const r = await client.requestRaw(
        `${ACM_API.executionOutput}?executionId=${encodeURIComponent(executionId)}&name=${encodeURIComponent(resolvedName)}`
      );
      if (r.status === 404)
        return instanceResult(instance, `Output '${outputName}' not found for execution '${executionId}'.`, true);
      if (r.status !== 200) return instanceResult(instance, `HTTP ${r.status} fetching output:\n${r.text.slice(0, 1000)}`, true);
      const isTexty = /text|json|xml|yaml|csv|javascript/.test(r.contentType) || !r.contentType;
      if (isTexty) return instanceResult(instance, `Output '${outputName}' (${r.contentType || "text"}):\n\n${r.text}`);
      return instanceResult(
        instance,
        `Output '${outputName}' is binary (${r.contentType}), base64:\n${Buffer.from(r.text, "binary").toString("base64").slice(0, 100000)}`
      );
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* ============================================================================
 * Skill: ACM Groovy scripting guide (tools/skills), as a prompt and resources
 * ========================================================================== */

server.registerPrompt(
  SKILL_NAME,
  {
    title: "ACM Groovy scripting guide",
    description: "How to write, validate and run ACM Groovy scripts safely.",
  },
  () => ({
    messages: [
      {
        role: "user" as const,
        content: {
          type: "text" as const,
          text: `${SKILL_DOCUMENTS[0].text}\n\nReferences are available as MCP resources under ${skillUri("")}.`,
        },
      },
    ],
  })
);

for (const doc of SKILL_DOCUMENTS) {
  server.registerResource(
    doc.path,
    skillUri(doc.path),
    { title: doc.title, description: doc.description, mimeType: "text/markdown" },
    (uri) => ({ contents: [{ uri: uri.href, mimeType: "text/markdown", text: doc.text }] })
  );
}

for (const template of SCRIPT_TEMPLATES) {
  server.registerResource(
    template.path,
    skillUri(template.path),
    { title: `${template.name} (${template.target.toLowerCase()} template)`, description: template.description, mimeType: "text/plain" },
    (uri) => ({ contents: [{ uri: uri.href, mimeType: "text/plain", text: template.code }] })
  );
}

/* ============================================================================
 * Bootstrap
 * ========================================================================== */

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  const enabled = enabledInstances(config.instances);
  const active = resolveActive(config.instances, config.activeInstance);
  console.error(
    `[acm-mcp] ready — ${enabled.length} enabled instance(s)${active ? `, active: ${active.name}` : ""}: ` +
      (enabled.length ? enabled.map((instance) => `${instance.name} (${instance.url})`).join(", ") : "(all disabled)")
  );
}

main().catch((e) => {
  console.error("[acm-mcp] fatal:", e);
  process.exit(1);
});
