#!/usr/bin/env node
/**
 * ============================================================================
 *  ACM MCP Server — Model Context Protocol server for AEM Content Manager
 *  (https://github.com/wttech/acm)
 * ============================================================================
 *
 *  Lets an MCP client (Claude Desktop, Claude Code, etc.) talk to ACM deployed
 *  on AEM (incl. AEM as a Cloud Service): validate Groovy code, execute it
 *  (queued with polling, or synchronously without recording history),
 *  inspect execution history, read console
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
 *  CONFIGURATION (environment variables)
 *  ----------------------------------------------------------------------------
 *    AEM_BASE_URL      e.g. https://author-pXXXX-eYYYY.adobeaemcloud.com
 *                      or   http://localhost:4502
 *
 *    AEM_AUTH          "bearer" | "cookie" | "basic"   (default: auto-detect)
 *
 *    AEM_TOKEN         Bearer token. For AEMaaCS the easiest user-bound token:
 *                      Cloud Manager -> Developer Console (of the environment)
 *                      -> Integrations -> Local token -> "Get Local Development
 *                      Token". Valid 24h, acts on behalf of YOUR user.
 *
 *    AEM_COOKIE_FILE   Alternative: path to a file holding the `login-token`
 *                      value. Re-read on every request, so refreshing the
 *                      token on disk takes effect without restarting the
 *                      server. Takes precedence over AEM_COOKIE.
 *    AEM_COOKIE        Alternative: value of the `login-token` cookie copied
 *                      from the browser (DevTools -> Application -> Cookies)
 *                      after logging into AEM author. Either the bare value or
 *                      the full "login-token=..." pair. CSRF tokens are
 *                      handled automatically in this mode.
 *
 *    AEM_USER /        Basic auth, for local AEM SDK (default admin:admin).
 *    AEM_PASSWORD
 *
 *    ACM_READONLY      "true" disables the tools that run or abort code
 *                      (validate/parse still allowed). A guardrail against
 *                      accidental runs, not a security boundary.
 *
 *    ACM_RUN_TIMEOUT_MS    Max time acm_run_code waits for completion before
 *                          returning the executionId for later polling.
 *                          Default 120000.
 *    ACM_POLL_INTERVAL_MS  Queue polling interval. Default 1500.
 *    AEM_HTTP_TIMEOUT_MS   Per-request HTTP timeout. Default 30000.
 *
 *  ----------------------------------------------------------------------------
 *  CLAUDE DESKTOP CONFIG EXAMPLE (claude_desktop_config.json)
 *  ----------------------------------------------------------------------------
 *    {
 *      "mcpServers": {
 *        "acm": {
 *          "command": "npx",
 *          "args": ["-y", "@wppes/acm-mcp-server"],
 *          "env": {
 *            "AEM_BASE_URL": "https://author-pXXXX-eYYYY.adobeaemcloud.com",
 *            "AEM_TOKEN": "eyJhbGciOi..."
 *          }
 *        }
 *      }
 *    }
 * ============================================================================
 */

import { readFileSync } from "node:fs";

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

/* ============================================================================
 * Configuration
 * ========================================================================== */

type AuthMode = "bearer" | "cookie" | "basic";

interface Config {
  baseUrl: string;
  authMode: AuthMode;
  token?: string;
  cookie?: string;
  cookieFile?: string;
  user?: string;
  password?: string;
  readonly: boolean;
  runTimeoutMs: number;
  pollIntervalMs: number;
  httpTimeoutMs: number;
}

/** Accepts either a bare `login-token` value or a full `name=value` pair. */
function normalizeCookie(raw: string | undefined): string | undefined {
  const value = raw?.trim();
  if (!value) return undefined;
  return value.includes("=") ? value : `login-token=${value}`;
}

function loadConfig(): Config {
  const baseUrl = (process.env.AEM_BASE_URL || "").replace(/\/+$/, "");
  if (!baseUrl) {
    console.error("[acm-mcp] AEM_BASE_URL is required.");
    process.exit(1);
  }

  const token = process.env.AEM_TOKEN?.trim() || undefined;
  const cookieFile = process.env.AEM_COOKIE_FILE?.trim() || undefined;
  const cookie = normalizeCookie(process.env.AEM_COOKIE);
  const user = process.env.AEM_USER || undefined;
  const password = process.env.AEM_PASSWORD || undefined;

  let authMode = (process.env.AEM_AUTH?.toLowerCase() as AuthMode) || undefined;
  if (!authMode) {
    if (token) authMode = "bearer";
    else if (cookie || cookieFile) authMode = "cookie";
    else if (user && password) authMode = "basic";
    else {
      console.error(
        "[acm-mcp] No credentials. Set AEM_TOKEN (AEMaaCS Developer Console " +
          "local token), AEM_COOKIE / AEM_COOKIE_FILE (browser login-token), " +
          "or AEM_USER/AEM_PASSWORD."
      );
      process.exit(1);
    }
  }

  return {
    baseUrl,
    authMode,
    token,
    cookie,
    cookieFile,
    user,
    password,
    readonly: /^(1|true|yes)$/i.test(process.env.ACM_READONLY || ""),
    runTimeoutMs: parseInt(process.env.ACM_RUN_TIMEOUT_MS || "120000", 10),
    pollIntervalMs: parseInt(process.env.ACM_POLL_INTERVAL_MS || "1500", 10),
    httpTimeoutMs: parseInt(process.env.AEM_HTTP_TIMEOUT_MS || "30000", 10),
  };
}

const config = loadConfig();

/* ============================================================================
 * ACM types (mirroring dev.vml.es.acm.core servlet contracts)
 * ========================================================================== */

/** Standard ACM API envelope: { status, message, data } */
interface ApiResponse<T = unknown> {
  status: number;
  message: string;
  data: T;
}

interface Execution {
  id: string;
  userId?: string;
  status: ExecutionStatus | string;
  startDate?: string;
  endDate?: string;
  duration?: number;
  output?: string;
  error?: string | null;
  executable?: { id: string; content?: string };
  instance?: unknown;
  [key: string]: unknown;
}

type ExecutionStatus =
  | "QUEUED"
  | "ACTIVE"
  | "PARSING"
  | "CHECKING"
  | "RUNNING"
  | "STOPPED"
  | "ABORTED"
  | "SKIPPED"
  | "LOCKED"
  | "FAILED"
  | "SUCCEEDED";

const PENDING_STATUSES = new Set(["QUEUED", "ACTIVE", "PARSING", "CHECKING", "RUNNING"]);

function isPending(status: string | undefined): boolean {
  return !!status && PENDING_STATUSES.has(status.toUpperCase());
}

const FAILED_STATUSES = new Set(["FAILED", "ABORTED", "LOCKED"]);

function isFailed(status: string | undefined): boolean {
  return !!status && FAILED_STATUSES.has(status.toUpperCase());
}

interface QueueOutput {
  executions: Execution[];
}

interface ExecutionListOutput {
  list: Execution[];
  [key: string]: unknown;
}

/* ============================================================================
 * HTTP client with bearer / cookie(+CSRF) / basic auth
 * ========================================================================== */

class AcmHttpError extends Error {
  constructor(
    public readonly httpStatus: number,
    message: string,
    public readonly body?: string
  ) {
    super(message);
  }
}

class AcmClient {
  private csrfToken: string | null = null;
  private csrfTokenCookie: string | undefined;

  constructor(private readonly cfg: Config) {}

  get baseUrl(): string {
    return this.cfg.baseUrl;
  }

  get authDescription(): string {
    switch (this.cfg.authMode) {
      case "bearer":
        return "Bearer token (IMS user token)";
      case "cookie":
        return "Browser login-token cookie (+ CSRF token)";
      case "basic":
        return `Basic auth (user: ${this.cfg.user})`;
    }
  }

  private baseHeaders(): Record<string, string> {
    const h: Record<string, string> = {
      Accept: "application/json",
      // Some environments enable the Sling referrer filter for non-GET
      // requests; sending a same-origin Referer is the standard workaround.
      Referer: this.cfg.baseUrl + "/",
    };
    switch (this.cfg.authMode) {
      case "bearer":
        h["Authorization"] = `Bearer ${this.cfg.token}`;
        break;
      case "cookie": {
        const cookie = this.currentCookie();
        if (!cookie) {
          throw new AcmHttpError(
            401,
            this.cfg.cookieFile
              ? `No login-token in ${this.cfg.cookieFile}. Write a fresh login-token value to it, or set AEM_COOKIE.`
              : "No login-token. Set AEM_COOKIE, or AEM_COOKIE_FILE pointing at a file holding the value."
          );
        }
        h["Cookie"] = cookie;
        break;
      }
      case "basic":
        h["Authorization"] =
          "Basic " + Buffer.from(`${this.cfg.user}:${this.cfg.password}`).toString("base64");
        break;
    }
    return h;
  }

  /**
   * The cookie in use right now. AEM_COOKIE_FILE is re-read on every call, so a
   * token refreshed on disk is picked up without restarting the server.
   */
  private currentCookie(): string | undefined {
    if (this.cfg.cookieFile) {
      try {
        const fromFile = normalizeCookie(readFileSync(this.cfg.cookieFile, "utf8"));
        if (fromFile) return fromFile;
      } catch {
        // unreadable or not written yet — fall back to AEM_COOKIE below
      }
    }
    return this.cfg.cookie;
  }

  /** Cookie-authenticated POST/DELETE on AEM require a Granite CSRF token. */
  private async ensureCsrfToken(force = false, timeoutMs?: number): Promise<string | null> {
    if (this.cfg.authMode !== "cookie") return null;
    const cookie = this.currentCookie();
    // A CSRF token belongs to one session, so a refreshed cookie invalidates it.
    if (cookie !== this.csrfTokenCookie) this.csrfToken = null;
    if (this.csrfToken && !force) return this.csrfToken;
    const res = await this.rawFetch(
      "/libs/granite/csrf/token.json",
      { method: "GET", headers: this.baseHeaders() },
      timeoutMs
    );
    if (!res.ok) {
      throw new AcmHttpError(
        res.status,
        `Failed to obtain CSRF token (HTTP ${res.status}). The login-token cookie may have expired — copy a fresh one from the browser.`
      );
    }
    const json = (await res.json()) as { token?: string };
    this.csrfToken = json.token || null;
    this.csrfTokenCookie = cookie;
    return this.csrfToken;
  }

  private async rawFetch(path: string, init: RequestInit, timeoutMs = this.cfg.httpTimeoutMs): Promise<Response> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      return await fetch(this.cfg.baseUrl + path, { ...init, signal: controller.signal });
    } finally {
      clearTimeout(timer);
    }
  }

  /**
   * Perform a request against an ACM endpoint and unwrap the
   * { status, message, data } envelope. Throws AcmHttpError with a
   * human-actionable message on auth failures.
   */
  async request<T>(
    method: "GET" | "POST" | "DELETE",
    path: string,
    body?: unknown,
    retryOnCsrf = true,
    timeoutMs?: number
  ): Promise<ApiResponse<T>> {
    // A timeout bounds the whole call, CSRF fetch and retry included.
    const deadline = timeoutMs !== undefined ? Date.now() + timeoutMs : undefined;
    const remaining = () => (deadline !== undefined ? Math.max(1, deadline - Date.now()) : undefined);
    const headers = this.baseHeaders();
    if (method !== "GET") {
      const csrf = await this.ensureCsrfToken(false, remaining());
      if (csrf) headers["CSRF-Token"] = csrf;
      if (body !== undefined) headers["Content-Type"] = "application/json";
    }

    const res = await this.rawFetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }, remaining());

    const text = await res.text();

    if (res.status === 401) {
      throw new AcmHttpError(
        401,
        this.cfg.authMode === "bearer"
          ? "401 Unauthorized — the bearer token is invalid or expired. AEMaaCS local development tokens expire after 24h: fetch a fresh one from Cloud Manager → Developer Console → Integrations → Local token, and update AEM_TOKEN."
          : this.cfg.authMode === "cookie"
            ? "401 Unauthorized — the login-token cookie is invalid or expired. Log into AEM author in the browser and copy a fresh cookie into AEM_COOKIE."
            : "401 Unauthorized — check AEM_USER / AEM_PASSWORD.",
        text
      );
    }
    if (res.status === 403) {
      // Could be missing ACM permissions (3-level: api node, feature node,
      // script path) — or, in cookie mode, a stale CSRF token.
      if (this.cfg.authMode === "cookie" && retryOnCsrf && method !== "GET") {
        await this.ensureCsrfToken(true, remaining());
        return this.request<T>(method, path, body, false, remaining());
      }
      throw new AcmHttpError(
        403,
        `403 Forbidden for ${path}. The user behind the credentials likely lacks ACM permissions. ACM authorizes at three levels (jcr:read required on each): the API node under /apps/acm/api, the feature node under /apps/acm/feature, and the script path under /conf/acm/settings/script. By default only administrators have access.`,
        text
      );
    }

    let json: ApiResponse<T>;
    try {
      json = JSON.parse(text) as ApiResponse<T>;
    } catch {
      throw new AcmHttpError(
        res.status,
        `Non-JSON response (HTTP ${res.status}) from ${path}. Is ACM installed on this instance? First 500 chars:\n${text.slice(0, 500)}`,
        text
      );
    }

    if (!res.ok) {
      throw new AcmHttpError(res.status, json.message || `HTTP ${res.status} from ${path}`, text);
    }
    return json;
  }

  /** GET that returns the raw body as text (for execution output download). */
  async requestRaw(path: string): Promise<{ status: number; contentType: string; text: string }> {
    const res = await this.rawFetch(path, { method: "GET", headers: this.baseHeaders() });
    const text = await res.text();
    if (res.status === 401 || res.status === 403) {
      throw new AcmHttpError(res.status, `HTTP ${res.status} fetching ${path}`, text);
    }
    return { status: res.status, contentType: res.headers.get("content-type") || "", text };
  }
}

const client = new AcmClient(config);

/* ============================================================================
 * ACM operations
 * ========================================================================== */

const CONSOLE_CODE_ID = "console";

/** Wrap bare Groovy in the canRun/doRun contract if the user didn't. */
function normalizeGroovy(code: string): string {
  if (/\bvoid\s+doRun\s*\(/.test(code)) return code;
  const indented = code
    .split("\n")
    .map((l) => (l.trim() ? "    " + l : l))
    .join("\n");
  return `boolean canRun() {\n    return conditions.always()\n}\n\nvoid doRun() {\n${indented}\n}`;
}

async function fetchExecutionById(executionId: string): Promise<Execution | null> {
  // 1) Try the queue (covers QUEUED/RUNNING and just-finished jobs).
  try {
    const q = await client.request<QueueOutput>(
      "GET",
      `/apps/acm/api/queue-code.json?executionId=${encodeURIComponent(executionId)}`
    );
    const found = q.data?.executions?.find((e) => e.id === executionId);
    if (found) return found;
  } catch (e) {
    if (!(e instanceof AcmHttpError && e.httpStatus === 404)) throw e;
  }
  // 2) Fall back to execution history.
  try {
    const h = await client.request<ExecutionListOutput>(
      "GET",
      `/apps/acm/api/execution.json?id=${encodeURIComponent(executionId)}&format=full`
    );
    const list = h.data?.list || [];
    return list.find((e) => e.id === executionId) || list[0] || null;
  } catch (e) {
    if (e instanceof AcmHttpError && e.httpStatus === 404) return null;
    throw e;
  }
}

async function fetchConsoleOutput(executionId: string): Promise<string | null> {
  try {
    const r = await client.requestRaw(
      `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(executionId)}&name=console`
    );
    return r.status === 200 ? r.text : null;
  } catch {
    return null;
  }
}

function summarizeExecution(e: Execution, consoleOutput?: string | null): string {
  const lines: string[] = [];
  lines.push(`Execution ID: ${e.id}`);
  lines.push(`Status:       ${e.status}`);
  if (e.executable?.id) lines.push(`Executable:   ${e.executable.id}`);
  if (e.userId) lines.push(`User:         ${e.userId}`);
  if (e.startDate) lines.push(`Started:      ${e.startDate}`);
  if (e.endDate) lines.push(`Ended:        ${e.endDate}`);
  if (e.duration !== undefined) lines.push(`Duration:     ${e.duration} ms`);
  if (e.error) lines.push(`\n--- ERROR ---\n${e.error}`);
  const out = consoleOutput ?? e.output;
  if (out && out.trim()) lines.push(`\n--- OUTPUT ---\n${out}`);
  return lines.join("\n");
}

function sleep(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms));
}

/* ============================================================================
 * MCP server & tools
 * ========================================================================== */

const { version } = JSON.parse(readFileSync(new URL("../package.json", import.meta.url), "utf8")) as {
  version: string;
};

const server = new McpServer({
  name: "acm-mcp-server",
  version,
});

function textResult(text: string, isError = false) {
  return { content: [{ type: "text" as const, text }], isError };
}

function errorResult(e: unknown) {
  const msg = e instanceof Error ? e.message : String(e);
  return textResult(`ERROR: ${msg}`, true);
}

const targetInfo = () =>
  `Target: ${client.baseUrl} | Auth: ${client.authDescription}${config.readonly ? " | READ-ONLY MODE" : ""}`;

/* ---------------------------------------------------------------- health -- */
server.registerTool(
  "acm_health",
  {
    title: "ACM health & state",
    description:
      "Check connectivity, authentication and ACM instance state (health checks, queued executions, instance info). Call this first to verify the setup. Returns the raw state from /apps/acm/api/state.json.",
    inputSchema: {},
  },
  async () => {
    try {
      const res = await client.request<unknown>("GET", "/apps/acm/api/state.json");
      return textResult(
        `${targetInfo()}\n\nACM state (healthy connection):\n${JSON.stringify(res.data, null, 2)}`
      );
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* -------------------------------------------------------------- validate -- */
server.registerTool(
  "acm_validate_code",
  {
    title: "Validate Groovy code (compile only)",
    description:
      "Compile-check Groovy code with ACM without executing it (mode=parse on /apps/acm/api/execute-code.json). Returns compile errors with line/column if any. ALWAYS validate code before running it. Never recorded in ACM execution history. Code is auto-wrapped in canRun()/doRun() if not already structured that way.",
    inputSchema: {
      code: z.string().describe("Groovy code. Either a full ACM script (canRun/doRun) or a bare snippet."),
    },
  },
  async ({ code }) => {
    try {
      const content = normalizeGroovy(code);
      const res = await client.request<Execution>("POST", "/apps/acm/api/execute-code.json", {
        mode: "parse",
        code: { id: CONSOLE_CODE_ID, content },
      });
      const execution = res.data;
      if (execution.error) {
        const m = execution.error.match(/@ line (\d+), column (\d+)/);
        const loc = m ? ` (line ${m[1]}, column ${m[2]} of the normalized script)` : "";
        return textResult(
          `COMPILE ERROR${loc}:\n${execution.error}\n\nNormalized script that was checked:\n${content}`,
          true
        );
      }
      return textResult("Code compiles successfully. ✔");
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
      "Run Groovy code on the AEM instance on behalf of the authenticated user and return the final status with full console output. By default (history=true) the code is queued (POST /apps/acm/api/queue-code.json) and polled until it finishes; the execution is recorded in ACM history, and if it is still running when the timeout elapses the executionId is returned — use acm_get_execution to keep checking. With history=false the code runs synchronously (POST /apps/acm/api/execute-code.json) and is NOT recorded in ACM history: use it while developing or debugging a script, for short read-only or dry-run runs, so repeated attempts do not flood the history. A history=false run has no trackable executionId, cannot be aborted, keeps no output files, and is cut off client-side after waitMs while the script may keep running on AEM. Run anything that changes content with history=true, so the change stays auditable. Prefer dry-run patterns (repo.dryRun) for destructive operations. Code is auto-wrapped in canRun()/doRun() if needed.",
    inputSchema: {
      code: z.string().describe("Groovy code to execute (full ACM script or bare snippet)."),
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
  async ({ code, inputs, waitMs, history }) => {
    if (config.readonly) {
      return textResult(
        "BLOCKED: ACM_READONLY is enabled for this server — code execution is disabled. Only validation and read tools are available.",
        true
      );
    }
    const content = normalizeGroovy(code);
    // InputValues on the server side is a flat HashMap<String, Object>
    const inputValues = inputs && Object.keys(inputs).length > 0 ? inputs : undefined;
    if (history === false) return runWithoutHistory(content, inputValues, waitMs ?? config.runTimeoutMs);
    try {
      const res = await client.request<QueueOutput>("POST", "/apps/acm/api/queue-code.json", {
        code: { id: CONSOLE_CODE_ID, content },
        inputs: inputValues,
      });

      let execution = res.data?.executions?.[0];
      if (!execution) {
        return textResult(`Queued, but no execution returned. Server message: ${res.message}`, true);
      }
      if (execution.status?.toUpperCase() === "SKIPPED") {
        return textResult(
          `Execution SKIPPED — canRun() returned false (or a CHECK-phase condition prevented the run).\n\n${summarizeExecution(execution)}`
        );
      }

      const deadline = Date.now() + (waitMs ?? config.runTimeoutMs);
      while (isPending(execution.status) && Date.now() < deadline) {
        await sleep(config.pollIntervalMs);
        const polled = await fetchExecutionById(execution.id);
        if (polled) execution = polled;
      }

      if (isPending(execution.status)) {
        return textResult(
          `Execution still ${execution.status} after ${waitMs ?? config.runTimeoutMs} ms.\nExecution ID: ${execution.id}\nUse acm_get_execution with this ID to check progress, or acm_abort_execution to stop it.`
        );
      }

      const consoleOut = await fetchConsoleOutput(execution.id);
      return textResult(summarizeExecution(execution, consoleOut), isFailed(execution.status));
    } catch (e) {
      return errorResult(e);
    }
  }
);

/**
 * Run code synchronously with history disabled. ACM's execute-code servlet
 * honours a per-request `history` flag; the queue servlet does not.
 */
async function runWithoutHistory(content: string, inputs: Record<string, unknown> | undefined, timeoutMs: number) {
  try {
    const res = await client.request<Execution>(
      "POST",
      "/apps/acm/api/execute-code.json",
      { mode: "RUN", history: false, code: { id: CONSOLE_CODE_ID, content }, inputs },
      true,
      timeoutMs
    );
    const execution = res.data;
    const note = "(Not recorded in ACM history.)";
    if (execution.status?.toUpperCase() === "SKIPPED") {
      return textResult(
        `Execution SKIPPED — canRun() returned false (or a CHECK-phase condition prevented the run). ${note}\n\n${summarizeExecution(execution)}`
      );
    }
    return textResult(`${summarizeExecution(execution)}\n\n${note}`, isFailed(execution.status));
  } catch (e) {
    if (e instanceof Error && e.name === "AbortError") {
      return textResult(
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
    },
  },
  async ({ executionId }) => {
    try {
      const execution = await fetchExecutionById(executionId);
      if (!execution) return textResult(`Execution '${executionId}' not found (queue or history).`, true);
      const consoleOut = isPending(execution.status) ? null : await fetchConsoleOutput(executionId);
      return textResult(summarizeExecution(execution, consoleOut));
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
      "Abort a queued or running execution (DELETE /apps/acm/api/queue-code.json?executionId=...). Scripts honoring context.checkAborted() will stop gracefully.",
    inputSchema: {
      executionId: z.string().describe("ID of the execution to abort."),
    },
  },
  async ({ executionId }) => {
    if (config.readonly) {
      return textResult("BLOCKED: ACM_READONLY is enabled — mutating operations are disabled.", true);
    }
    try {
      const res = await client.request<unknown>(
        "DELETE",
        `/apps/acm/api/queue-code.json?executionId=${encodeURIComponent(executionId)}`
      );
      return textResult(`Abort requested. Server message: ${res.message}`);
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
      "List past executions from ACM history (/apps/acm/api/execution.json), newest first, in summary format. Optionally filter to currently queued executions only.",
    inputSchema: {
      limit: z.number().int().positive().max(200).optional().describe("Max results (default 20)."),
      offset: z.number().int().nonnegative().optional().describe("Pagination offset (default 0)."),
      queuedOnly: z.boolean().optional().describe("If true, list only currently queued/running executions."),
    },
  },
  async ({ limit, offset, queuedOnly }) => {
    try {
      const params = new URLSearchParams({ format: "summary" });
      params.set("limit", String(limit ?? 20));
      if (offset) params.set("offset", String(offset));
      if (queuedOnly) params.set("queued", "true");
      const res = await client.request<ExecutionListOutput>(
        "GET",
        `/apps/acm/api/execution.json?${params.toString()}`
      );
      const list = res.data?.list || [];
      if (list.length === 0) return textResult("No executions found.");
      const rows = list.map(
        (e) =>
          `${e.id} | ${e.status} | ${e.executable?.id ?? "?"} | user=${e.userId ?? "?"} | start=${e.startDate ?? "?"} | ${e.duration ?? "?"} ms${e.error ? " | ERROR" : ""}`
      );
      return textResult(`Executions (${list.length}):\n` + rows.join("\n"));
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
      "List Groovy scripts stored on the instance under /conf/acm/settings/script (GET /apps/acm/api/script.json?type=...).",
    inputSchema: {
      type: z
        .enum(["MANUAL", "AUTOMATIC", "ENABLED", "DISABLED", "EXTENSION", "MOCK"])
        .optional()
        .describe("Script type/tab to list (default MANUAL)."),
    },
  },
  async ({ type }) => {
    try {
      const res = await client.request<{ list?: Array<{ id: string; path?: string; [k: string]: unknown }> }>(
        "GET",
        `/apps/acm/api/script.json?type=${encodeURIComponent(type ?? "MANUAL")}`
      );
      const list = res.data?.list || [];
      if (list.length === 0) return textResult(`No ${type ?? "MANUAL"} scripts found.`);
      return textResult(
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
    },
  },
  async ({ id }) => {
    try {
      const res = await client.request<{ list?: Array<{ id: string; content?: string }> }>(
        "GET",
        `/apps/acm/api/script.json?id=${encodeURIComponent(id)}`
      );
      const script = res.data?.list?.[0];
      if (!script) return textResult(`Script '${id}' not found.`, true);
      return textResult(`Script: ${script.id}\n\n${script.content ?? "(no content)"}`);
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
      "Resolve the inputs a script declares in describeRun() (POST /apps/acm/api/describe-code.json). Use this before acm_run_code for scripts with inputs, to learn names, types and defaults.",
    inputSchema: {
      code: z.string().describe("Groovy code (full ACM script) whose inputs should be described."),
    },
  },
  async ({ code }) => {
    // ACM runs the script's describeRun() to resolve inputs, so this executes code.
    if (config.readonly) {
      return textResult(
        "BLOCKED: ACM_READONLY is enabled — describing inputs runs the script's describeRun() on AEM, so it is disabled.",
        true
      );
    }
    try {
      const res = await client.request<unknown>("POST", "/apps/acm/api/describe-code.json", {
        code: { id: CONSOLE_CODE_ID, content: code },
      });
      return textResult(`Description:\n${JSON.stringify(res.data, null, 2)}`);
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
      "Fetch a named output of an execution from /apps/acm/api/execution-output.json — 'console' for console text, or the name given to outputs.file(...)/outputs.text(...) in the script (e.g. 'report'). Returns text content; binary outputs are returned base64-encoded.",
    inputSchema: {
      executionId: z.string().describe("Execution ID."),
      name: z.string().describe("Output name: 'console', 'archive', or a script-defined output name."),
    },
  },
  async ({ executionId, name }) => {
    try {
      const r = await client.requestRaw(
        `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(executionId)}&name=${encodeURIComponent(name)}`
      );
      if (r.status === 404) return textResult(`Output '${name}' not found for execution '${executionId}'.`, true);
      if (r.status !== 200) return textResult(`HTTP ${r.status} fetching output:\n${r.text.slice(0, 1000)}`, true);
      const isTexty = /text|json|xml|yaml|csv|javascript/.test(r.contentType) || !r.contentType;
      if (isTexty) return textResult(`Output '${name}' (${r.contentType || "text"}):\n\n${r.text}`);
      return textResult(
        `Output '${name}' is binary (${r.contentType}), base64:\n${Buffer.from(r.text, "binary").toString("base64").slice(0, 100000)}`
      );
    } catch (e) {
      return errorResult(e);
    }
  }
);

/* ============================================================================
 * Prompt: ACM Groovy conventions (helps the model write valid scripts)
 * ========================================================================== */

server.registerPrompt(
  "acm-scripting-guide",
  {
    title: "ACM Groovy scripting conventions",
    description: "Cheat sheet for writing ACM-compatible Groovy scripts.",
  },
  () => ({
    messages: [
      {
        role: "user" as const,
        content: {
          type: "text" as const,
          text: [
            "When writing Groovy for ACM (AEM Content Manager, wttech/acm), follow these conventions:",
            "",
            "Structure: scripts define `boolean canRun()` (use `conditions.always()` for ad-hoc runs) and `void doRun()`; optional `void describeRun()` declares inputs via the `inputs` service.",
            "Output: `println` for plain text; `out.info/success/warn/error` for timestamped console messages; `log.*` also writes to AEM logs. Generated files: `outputs.file(name){...}` / `outputs.text(name){...}`.",
            "Repository: prefer the `repo` service (`repo.get(path)`, `.ensureFolder()`, `.save(props)`, `.query(...)`, `repo.dryRun(boolean){...}`) over raw JCR APIs.",
            "Permissions: use the idempotent `acl` service (`acl.createUser{...}`, `acl.createGroup{...}`, `allow(path, perms)`).",
            "Long loops: call `context.checkAborted()` periodically so executions can be aborted gracefully.",
            "Safety: for destructive changes default to a `dryRun` boolean input set to true, wrap mutations in `repo.dryRun(...)`, and print a summary of what would change.",
          ].join("\n"),
        },
      },
    ],
  })
);

/* ============================================================================
 * Bootstrap
 * ========================================================================== */

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error(`[acm-mcp] ready — ${targetInfo()}`);
}

main().catch((e) => {
  console.error("[acm-mcp] fatal:", e);
  process.exit(1);
});
