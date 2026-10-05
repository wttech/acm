#!/usr/bin/env node
/**
 * End-to-end test for the ACM MCP server (built dist/index.js).
 *
 * Spawns the real MCP server over stdio (exactly like Claude Desktop would),
 * pointed at the mock ACM backend (test/mock-acm.mjs), and drives it through
 * the MCP SDK client: handshake, tool listing, and every tool call.
 *
 * Usage:  node test/mock-acm.mjs &   (or let this script spawn it)
 *         node test/run-tests.mjs
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { spawn } from "node:child_process";
import { rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";

const PORT = 45102;
const BASE_URL = `http://localhost:${PORT}`;
const SERVER_PATH = new URL("../dist/index.js", import.meta.url).pathname;

let passed = 0;
let failed = 0;
const failures = [];

function check(name, cond, detail = "") {
  if (cond) {
    passed += 1;
    console.log(`  ✔ ${name}`);
  } else {
    failed += 1;
    failures.push(`${name}${detail ? ` — ${detail}` : ""}`);
    console.log(`  ✘ ${name}${detail ? `\n      ${detail}` : ""}`);
  }
}

function text(result) {
  return (result.content || []).map((c) => c.text ?? "").join("\n");
}

async function connect(env) {
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: [SERVER_PATH],
    env: { ...process.env, ...env },
    stderr: "pipe",
  });
  const client = new Client({ name: "acm-test-client", version: "1.0.0" });
  await client.connect(transport);
  return client;
}

async function main() {
  // -- start mock ACM backend ------------------------------------------------
  const mock = spawn("node", [new URL("./mock-acm.mjs", import.meta.url).pathname], {
    stdio: ["ignore", "pipe", "inherit"],
  });
  await new Promise((resolve, reject) => {
    mock.stdout.on("data", (d) => d.toString().includes("listening") && resolve());
    mock.on("exit", (c) => reject(new Error(`mock exited early (${c})`)));
    setTimeout(() => reject(new Error("mock did not start in 5s")), 5000);
  });
  console.log("mock ACM backend up\n");

  try {
    /* ====================== basic auth, read-write mode ==================== */
    console.log("— connect (basic auth) + MCP handshake");
    const client = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_USER: "admin",
      AEM_PASSWORD: "admin",
      ACM_POLL_INTERVAL_MS: "100",
    });
    check("MCP handshake (initialize) succeeded", true);

    const tools = await client.listTools();
    const toolNames = tools.tools.map((t) => t.name).sort();
    console.log(`  tools: ${toolNames.join(", ")}`);
    const expected = [
      "acm_abort_execution",
      "acm_describe_inputs",
      "acm_get_execution",
      "acm_get_output_file",
      "acm_get_script",
      "acm_health",
      "acm_list_executions",
      "acm_list_scripts",
      "acm_run_code",
      "acm_validate_code",
    ];
    check(
      `tools/list returns all 10 tools`,
      JSON.stringify(toolNames) === JSON.stringify(expected),
      `got: ${toolNames.join(",")}`
    );

    const prompts = await client.listPrompts();
    check(
      "prompts/list returns acm-scripting-guide",
      prompts.prompts.some((p) => p.name === "acm-scripting-guide")
    );
    const prompt = await client.getPrompt({ name: "acm-scripting-guide" });
    check(
      "prompts/get returns scripting conventions",
      prompt.messages?.[0]?.content?.text?.includes("canRun()")
    );

    console.log("\n— acm_health");
    let r = await client.callTool({ name: "acm_health", arguments: {} });
    check("health reports healthy connection", !r.isError && text(r).includes("healthy connection"), text(r));
    check("health includes target info", text(r).includes(BASE_URL));

    console.log("\n— acm_validate_code");
    r = await client.callTool({
      name: "acm_validate_code",
      arguments: { code: 'println "ok"' },
    });
    check("valid bare snippet compiles (auto-wrap)", !r.isError && text(r).includes("compiles successfully"), text(r));

    r = await client.callTool({
      name: "acm_validate_code",
      arguments: { code: "NOT_GROOVY at all" },
    });
    check("invalid code returns compile error with location", r.isError === true && /line 6, column 5/.test(text(r)), text(r));

    console.log("\n— acm_run_code (queue + poll to completion)");
    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "hello from mock ACM"\nprintln 42' },
    });
    check("run reaches SUCCEEDED", !r.isError && text(r).includes("SUCCEEDED"), text(r));
    check("run returns console output", text(r).includes("hello from mock ACM"), text(r));
    const runId = text(r).match(/Execution ID: (\S+)/)?.[1];
    check("run returns execution id", !!runId);

    console.log("\n— acm_run_code (runtime failure)");
    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: "FAIL_AT_RUNTIME" },
    });
    check("failed run is FAILED + isError", r.isError === true && text(r).includes("FAILED"), text(r));
    check("failed run surfaces error message", text(r).includes("IllegalStateException"), text(r));

    console.log("\n— acm_run_code (history=false, synchronous)");
    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "debug run"', history: false },
    });
    check("run without history reaches SUCCEEDED", !r.isError && text(r).includes("SUCCEEDED"), text(r));
    check("run without history returns console output", text(r).includes("hello from mock ACM"), text(r));
    check("run without history says it is not recorded", text(r).includes("Not recorded in ACM history"), text(r));
    const debugId = text(r).match(/Execution ID: (\S+)/)?.[1];
    r = await client.callTool({ name: "acm_list_executions", arguments: { limit: 50 } });
    check("run without history is absent from history", !!debugId && !text(r).includes(debugId), text(r));

    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: "FAIL_AT_RUNTIME", history: false },
    });
    check("failed run without history is FAILED + isError", r.isError === true && text(r).includes("IllegalStateException"), text(r));

    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: "RUN_FOREVER", history: false, waitMs: 500 },
    });
    check(
      "slow run without history times out with a rerun hint",
      r.isError === true && text(r).includes("history=true"),
      text(r)
    );

    console.log("\n— LOCKED is an error on both paths");
    r = await client.callTool({ name: "acm_run_code", arguments: { code: "LOCKED_RUN" } });
    check("queued LOCKED run is isError", r.isError === true && text(r).includes("LOCKED"), text(r));
    r = await client.callTool({ name: "acm_run_code", arguments: { code: "LOCKED_RUN", history: false } });
    check("LOCKED run without history is isError", r.isError === true && text(r).includes("LOCKED"), text(r));

    console.log("\n— acm_get_execution");
    r = await client.callTool({ name: "acm_get_execution", arguments: { executionId: runId } });
    check("get_execution finds finished execution", !r.isError && text(r).includes("SUCCEEDED"), text(r));
    r = await client.callTool({ name: "acm_get_execution", arguments: { executionId: "nope/123" } });
    check("get_execution: missing id -> not found error", r.isError === true && text(r).includes("not found"), text(r));

    console.log("\n— acm_run_code timeout + acm_abort_execution");
    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: "RUN_FOREVER", waitMs: 500 },
    });
    check("long run returns with pending status + id", !r.isError && /still (RUNNING|QUEUED)/.test(text(r)), text(r));
    const longId = text(r).match(/Execution ID: (\S+)/)?.[1];
    r = await client.callTool({ name: "acm_abort_execution", arguments: { executionId: longId } });
    check("abort requested ok", !r.isError && text(r).toLowerCase().includes("abort"), text(r));
    r = await client.callTool({ name: "acm_get_execution", arguments: { executionId: longId } });
    check("aborted execution shows ABORTED", text(r).includes("ABORTED"), text(r));

    console.log("\n— acm_list_executions");
    r = await client.callTool({ name: "acm_list_executions", arguments: { limit: 10 } });
    check("history lists finished executions", !r.isError && text(r).includes(runId), text(r));

    console.log("\n— acm_list_scripts / acm_get_script");
    r = await client.callTool({ name: "acm_list_scripts", arguments: {} });
    check("lists MANUAL scripts", !r.isError && text(r).includes("hello.groovy"), text(r));
    r = await client.callTool({
      name: "acm_get_script",
      arguments: { id: "/conf/acm/settings/script/manual/example/hello.groovy" },
    });
    check("reads script content", !r.isError && text(r).includes("hello from stored script"), text(r));
    r = await client.callTool({ name: "acm_get_script", arguments: { id: "/conf/acm/missing.groovy" } });
    check("missing script -> not found error", r.isError === true && text(r).includes("not found"), text(r));

    console.log("\n— acm_describe_inputs");
    r = await client.callTool({
      name: "acm_describe_inputs",
      arguments: { code: "void describeRun() { inputs.bool('dryRun') }" },
    });
    check("describe returns declared inputs", !r.isError && text(r).includes("dryRun"), text(r));

    console.log("\n— acm_get_output_file");
    r = await client.callTool({
      name: "acm_get_output_file",
      arguments: { executionId: runId, name: "console" },
    });
    check("downloads console output", !r.isError && text(r).includes("hello from mock ACM"), text(r));
    r = await client.callTool({
      name: "acm_get_output_file",
      arguments: { executionId: runId, name: "report" },
    });
    check("downloads named csv output", !r.isError && text(r).includes("/content/foo"), text(r));
    r = await client.callTool({
      name: "acm_get_output_file",
      arguments: { executionId: runId, name: "missing" },
    });
    check("missing output -> not found error", r.isError === true, text(r));

    await client.close();

    /* ====================== cookie auth + CSRF ============================ */
    console.log("\n— cookie auth mode (login-token + CSRF)");
    const cookieClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_COOKIE: "mock-login-token", // bare value: server should prefix login-token=
      ACM_POLL_INTERVAL_MS: "100",
    });
    r = await cookieClient.callTool({ name: "acm_health", arguments: {} });
    check("cookie auth: health ok", !r.isError, text(r));
    r = await cookieClient.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "cookie run"' },
    });
    check("cookie auth: POST with CSRF token succeeds", !r.isError && text(r).includes("SUCCEEDED"), text(r));
    await cookieClient.close();

    const slowCsrfClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_COOKIE: "mock-login-token-slow-csrf",
    });
    const started = Date.now();
    r = await slowCsrfClient.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "x"', history: false, waitMs: 500 },
    });
    const elapsed = Date.now() - started;
    check(
      "cookie auth: waitMs bounds a run without history, CSRF fetch included",
      r.isError === true && text(r).includes("history=true") && elapsed < 5000,
      `${elapsed} ms: ${text(r)}`
    );
    await slowCsrfClient.close();

    /* ====================== cookie file =================================== */
    console.log("\n— cookie file (AEM_COOKIE_FILE)");
    const tokenFile = join(tmpdir(), `acm-test-token-${process.pid}`);
    writeFileSync(tokenFile, "stale-login-token\n");
    const fileClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_COOKIE_FILE: tokenFile,
      ACM_POLL_INTERVAL_MS: "100",
    });
    r = await fileClient.callTool({ name: "acm_health", arguments: {} });
    check("cookie file: stale token is rejected", r.isError === true, text(r));

    // The point of the file: refresh it under a running server, no restart.
    writeFileSync(tokenFile, "mock-login-token\n");
    r = await fileClient.callTool({ name: "acm_health", arguments: {} });
    check("cookie file: refreshed token picked up without a restart", !r.isError, text(r));

    r = await fileClient.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "cookie file run"' },
    });
    check(
      "cookie file: POST with a CSRF token succeeds after a refresh",
      !r.isError && text(r).includes("SUCCEEDED"),
      text(r)
    );
    await fileClient.close();
    rmSync(tokenFile, { force: true });

    const fallbackClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_COOKIE: "mock-login-token",
      AEM_COOKIE_FILE: join(tmpdir(), `acm-test-absent-${process.pid}`),
      ACM_POLL_INTERVAL_MS: "100",
    });
    r = await fallbackClient.callTool({ name: "acm_health", arguments: {} });
    check("cookie file: an unreadable file falls back to AEM_COOKIE", !r.isError, text(r));
    await fallbackClient.close();

    const emptyFileClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_COOKIE_FILE: join(tmpdir(), `acm-test-never-written-${process.pid}`),
      ACM_POLL_INTERVAL_MS: "100",
    });
    r = await emptyFileClient.callTool({ name: "acm_health", arguments: {} });
    check(
      "cookie file: a file that does not exist yet names itself in the error",
      r.isError === true && text(r).includes("acm-test-never-written"),
      text(r)
    );
    await emptyFileClient.close();

    /* ====================== bearer auth ==================================== */
    console.log("\n— bearer auth mode");
    const bearerClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_TOKEN: "mock-bearer-token",
      ACM_POLL_INTERVAL_MS: "100",
    });
    r = await bearerClient.callTool({ name: "acm_health", arguments: {} });
    check("bearer auth: health ok", !r.isError, text(r));
    await bearerClient.close();

    /* ====================== bad credentials ================================ */
    console.log("\n— bad credentials");
    const badClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_TOKEN: "wrong-token",
    });
    r = await badClient.callTool({ name: "acm_health", arguments: {} });
    check(
      "401 produces actionable message (Developer Console hint)",
      r.isError === true && text(r).includes("Developer Console"),
      text(r)
    );
    await badClient.close();

    /* ====================== read-only mode ================================ */
    console.log("\n— ACM_READONLY mode");
    const roClient = await connect({
      AEM_BASE_URL: BASE_URL,
      AEM_USER: "admin",
      AEM_PASSWORD: "admin",
      ACM_READONLY: "true",
    });
    r = await roClient.callTool({ name: "acm_run_code", arguments: { code: 'println "x"' } });
    check("readonly blocks acm_run_code", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await roClient.callTool({ name: "acm_run_code", arguments: { code: 'println "x"', history: false } });
    check("readonly blocks acm_run_code without history", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await roClient.callTool({ name: "acm_abort_execution", arguments: { executionId: "x" } });
    check("readonly blocks acm_abort_execution", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await roClient.callTool({ name: "acm_describe_inputs", arguments: { code: "void describeRun() {}" } });
    check("readonly blocks acm_describe_inputs", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await roClient.callTool({ name: "acm_validate_code", arguments: { code: 'println "x"' } });
    check("readonly still allows validation", !r.isError && text(r).includes("compiles"), text(r));
    r = await roClient.callTool({ name: "acm_health", arguments: {} });
    check("readonly: health shows READ-ONLY MODE banner", text(r).includes("READ-ONLY MODE"), text(r));
    await roClient.close();
  } finally {
    mock.kill();
    await sleep(100);
  }

  console.log(`\n========================================`);
  console.log(`PASSED: ${passed}   FAILED: ${failed}`);
  if (failures.length) {
    console.log("\nFailures:");
    for (const f of failures) console.log(`  - ${f}`);
  }
  process.exit(failed ? 1 : 0);
}

main().catch((e) => {
  console.error("test runner crashed:", e);
  process.exit(2);
});
