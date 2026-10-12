#!/usr/bin/env node
/**
 * End-to-end test for the ACM MCP server (built dist/index.js).
 *
 * Spawns the real MCP server over stdio (exactly like Claude Desktop would),
 * pointed at mock ACM backends (test/mock-acm.mjs), and drives it through
 * the MCP SDK client: handshake, tool listing, and every tool call.
 *
 * Usage:  node test/mock-acm.mjs &   (or let this script spawn it)
 *         node test/run-tests.mjs
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { spawn, spawnSync } from "node:child_process";
import { rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";

const PORT = 45102;
const PORT2 = 45103;
const BASE_URL = `http://localhost:${PORT}`;
const BASE_URL2 = `http://localhost:${PORT2}`;
const SERVER_PATH = new URL("../dist/index.js", import.meta.url).pathname;
const MOCK_PATH = new URL("./mock-acm.mjs", import.meta.url).pathname;

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

const basic = (name, url, extra = {}) => ({ name, url, authMode: "basic", user: "admin", secret: "admin", ...extra });

const config = (instances, extra = {}) =>
  JSON.stringify({ "acm.instances": instances, "acm.run.pollInterval": 100, ...extra });

async function connect(configJson, env = {}) {
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: [SERVER_PATH],
    env: { ...process.env, ACM_CONFIG: configJson, ...env },
    stderr: "pipe",
  });
  const client = new Client({ name: "acm-test-client", version: "1.0.0" });
  await client.connect(transport);
  return client;
}

function spawnMock(port, id) {
  const mock = spawn("node", [MOCK_PATH], {
    stdio: ["ignore", "pipe", "inherit"],
    env: { ...process.env, PORT: String(port), MOCK_ID: id },
  });
  return new Promise((resolvePromise, reject) => {
    mock.stdout.on("data", (d) => d.toString().includes("listening") && resolvePromise(mock));
    mock.on("exit", (c) => reject(new Error(`mock exited early (${c})`)));
    setTimeout(() => reject(new Error("mock did not start in 5s")), 5000);
  });
}

async function main() {
  // -- start mock ACM backends ----------------------------------------------
  const mocks = await Promise.all([spawnMock(PORT, "mock-one"), spawnMock(PORT2, "mock-two")]);
  console.log("mock ACM backends up\n");

  try {
    /* ================== two instances, "one" active ======================== */
    console.log("— connect (two instances, basic auth) + MCP handshake");
    const client = await connect(
      config(
        [
          basic("one", BASE_URL),
          basic("two", BASE_URL2),
          basic("ro", BASE_URL, { readonly: true }),
          basic("off", BASE_URL, { enabled: false }),
        ],
        { "acm.activeInstance": "one" }
      )
    );
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
      "acm_list_instances",
      "acm_list_scripts",
      "acm_run_code",
      "acm_validate_code",
    ];
    check(
      `tools/list returns all 11 tools`,
      JSON.stringify(toolNames) === JSON.stringify(expected),
      `got: ${toolNames.join(",")}`
    );

    const prompts = await client.listPrompts();
    check(
      "prompts/list returns acm-groovy-script",
      prompts.prompts.some((p) => p.name === "acm-groovy-script")
    );
    const prompt = await client.getPrompt({ name: "acm-groovy-script" });
    check(
      "prompts/get returns the scripting guide",
      prompt.messages?.[0]?.content?.text?.includes("canRun()")
    );

    const resources = await client.listResources();
    check(
      "resources/list returns the generated API reference",
      resources.resources.some((res) => res.uri === "acm://skill/references/api.md")
    );
    const apiReference = await client.readResource({ uri: "acm://skill/references/api.md" });
    check(
      "resources/read returns the API reference",
      apiReference.contents?.[0]?.text?.includes("### Repo")
    );
    check(
      "initialize returns the essentials as instructions",
      client.getInstructions()?.includes("context.checkAborted()")
    );

    console.log("\n— acm_list_instances");
    let r = await client.callTool({ name: "acm_list_instances", arguments: {} });
    check("lists enabled instances with the active one marked", !r.isError && /one \(active\)/.test(text(r)) && text(r).includes("two"), text(r));
    check("marks the read-only instance", text(r).includes("ro") && /ro.*read-only/.test(text(r)), text(r));
    check("hides the disabled instance", !text(r).includes("off"), text(r));

    console.log("\n— acm_health (all instances)");
    r = await client.callTool({ name: "acm_health", arguments: {} });
    check("health without instance checks both instances", text(r).includes(BASE_URL) && text(r).includes(BASE_URL2), text(r));
    check("health shows each instance's own state", text(r).includes("mock-one") && text(r).includes("mock-two"), text(r));
    check("health shows the read-only banner", text(r).includes("READ-ONLY MODE"), text(r));
    r = await client.callTool({ name: "acm_health", arguments: { instance: "two" } });
    check("health on a named instance only", text(r).includes("mock-two") && !text(r).includes("mock-one"), text(r));

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
    check("run names the active instance", text(r).includes("Instance: one"), text(r));
    const runId = text(r).match(/Execution ID: (\S+)/)?.[1];
    check("run returns execution id", !!runId);

    console.log("\n— acm_run_code (instance argument)");
    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "second instance"', instance: "two" },
    });
    check("run on the named instance", !r.isError && text(r).includes("Instance: two"), text(r));
    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "x"', instance: "off" },
    });
    check("disabled instance is rejected", r.isError === true && text(r).includes("off") && text(r).includes("one"), text(r));
    r = await client.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "x"', instance: "nope" },
    });
    check("unknown instance is rejected", r.isError === true && text(r).includes("nope"), text(r));

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

    console.log("\n— readonly instance");
    r = await client.callTool({ name: "acm_run_code", arguments: { code: 'println "x"', instance: "ro" } });
    check("readonly blocks acm_run_code", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await client.callTool({ name: "acm_run_code", arguments: { code: 'println "x"', history: false, instance: "ro" } });
    check("readonly blocks acm_run_code without history", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await client.callTool({ name: "acm_abort_execution", arguments: { executionId: "x", instance: "ro" } });
    check("readonly blocks acm_abort_execution", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await client.callTool({ name: "acm_describe_inputs", arguments: { code: "void describeRun() {}", instance: "ro" } });
    check("readonly blocks acm_describe_inputs", r.isError === true && text(r).includes("BLOCKED"), text(r));
    r = await client.callTool({ name: "acm_validate_code", arguments: { code: 'println "x"', instance: "ro" } });
    check("readonly still allows validation", !r.isError && text(r).includes("compiles"), text(r));

    await client.close();

    /* ====================== no active instance ============================ */
    console.log("\n— several enabled instances, no active one");
    const noActiveClient = await connect(config([basic("one", BASE_URL), basic("two", BASE_URL2)]));
    r = await noActiveClient.callTool({ name: "acm_run_code", arguments: { code: 'println "x"' } });
    check(
      "calls without instance are rejected listing names",
      r.isError === true && text(r).includes('"instance"') && text(r).includes("one") && text(r).includes("two"),
      text(r)
    );
    r = await noActiveClient.callTool({ name: "acm_run_code", arguments: { code: 'println "x"', instance: "two" } });
    check("naming the instance still works", !r.isError && text(r).includes("Instance: two"), text(r));
    await noActiveClient.close();

    /* ====================== single instance, implicit default ============== */
    console.log("\n— a single instance is the default, no secret needed for local admin");
    const singleClient = await connect(config([{ name: "local", url: BASE_URL, authMode: "basic", user: "admin" }]));
    r = await singleClient.callTool({ name: "acm_health", arguments: {} });
    check("local admin defaults to admin/admin and needs no instance arg", !r.isError && text(r).includes("mock-one"), text(r));
    await singleClient.close();

    /* ====================== cookie auth + CSRF ============================ */
    console.log("\n— cookie auth mode (login-token + CSRF)");
    const cookieClient = await connect(
      config([{ name: "cookie", url: BASE_URL, authMode: "cookie", secret: "mock-login-token" }])
    );
    r = await cookieClient.callTool({ name: "acm_health", arguments: {} });
    check("cookie auth: health ok", !r.isError, text(r));
    r = await cookieClient.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "cookie run"' },
    });
    check("cookie auth: POST with CSRF token succeeds", !r.isError && text(r).includes("SUCCEEDED"), text(r));
    await cookieClient.close();

    const slowCsrfClient = await connect(
      config([{ name: "cookie", url: BASE_URL, authMode: "cookie", secret: "mock-login-token-slow-csrf" }])
    );
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

    /* ====================== secret forms: env, file, missing =============== */
    console.log("\n— secret forms");
    const envClient = await connect(
      config([{ name: "env", url: BASE_URL, authMode: "basic", user: "admin", secret: { env: "ACM_TEST_SECRET" } }]),
      { ACM_TEST_SECRET: "admin" }
    );
    r = await envClient.callTool({ name: "acm_health", arguments: {} });
    check("secret from an env var works", !r.isError, text(r));
    r = await envClient.callTool({ name: "acm_list_instances", arguments: {} });
    check("instance shows credentials configured", text(r).includes("credentials: configured"), text(r));
    await envClient.close();

    const noSecretClient = await connect(config([basic("remote", "http://unreachable.example.com:9999", { secret: undefined })]));
    r = await noSecretClient.callTool({ name: "acm_list_instances", arguments: {} });
    check("instance without a secret shows MISSING credentials", text(r).includes("credentials: MISSING"), text(r));
    await noSecretClient.close();

    const tokenFile = join(tmpdir(), `acm-test-token-${process.pid}`);
    writeFileSync(tokenFile, "stale-login-token\n");
    const fileClient = await connect(
      config([{ name: "cookie", url: BASE_URL, authMode: "cookie", secret: { file: tokenFile } }])
    );
    r = await fileClient.callTool({ name: "acm_health", arguments: {} });
    check("secret file: stale token is rejected", r.isError === true, text(r));

    // The point of the file: refresh it under a running server, no restart.
    writeFileSync(tokenFile, "mock-login-token\n");
    r = await fileClient.callTool({ name: "acm_health", arguments: {} });
    check("secret file: refreshed token picked up without a restart", !r.isError, text(r));

    r = await fileClient.callTool({
      name: "acm_run_code",
      arguments: { code: 'println "cookie file run"' },
    });
    check(
      "secret file: POST with a CSRF token succeeds after a refresh",
      !r.isError && text(r).includes("SUCCEEDED"),
      text(r)
    );
    await fileClient.close();
    rmSync(tokenFile, { force: true });

    const absentFile = join(tmpdir(), `acm-test-never-written-${process.pid}`);
    const emptyFileClient = await connect(
      config([{ name: "cookie", url: BASE_URL, authMode: "cookie", secret: { file: absentFile } }])
    );
    r = await emptyFileClient.callTool({ name: "acm_health", arguments: {} });
    check(
      "secret file: a file that does not exist names itself in the error",
      r.isError === true && text(r).includes("acm-test-never-written"),
      text(r)
    );
    await emptyFileClient.close();

    /* ====================== config file source ============================= */
    console.log("\n— ACM_CONFIG_FILE");
    const fileConfig = join(tmpdir(), `acm-test-config-${process.pid}.json`);
    writeFileSync(fileConfig, config([basic("file", BASE_URL)]));
    const fileConfigClient = await connect("", { ACM_CONFIG: "", ACM_CONFIG_FILE: fileConfig });
    r = await fileConfigClient.callTool({ name: "acm_health", arguments: {} });
    check("config read from a file works", !r.isError && text(r).includes("mock-one"), text(r));
    await fileConfigClient.close();
    rmSync(fileConfig, { force: true });

    /* ====================== bearer auth ==================================== */
    console.log("\n— bearer auth mode");
    const bearerClient = await connect(
      config([{ name: "bearer", url: BASE_URL, authMode: "bearer", secret: "mock-bearer-token" }])
    );
    r = await bearerClient.callTool({ name: "acm_health", arguments: {} });
    check("bearer auth: health ok", !r.isError, text(r));
    await bearerClient.close();

    /* ====================== bad credentials ================================ */
    console.log("\n— bad credentials");
    const badClient = await connect(
      config([{ name: "bearer", url: BASE_URL, authMode: "bearer", secret: "wrong-token" }])
    );
    r = await badClient.callTool({ name: "acm_health", arguments: {} });
    check(
      "401 produces actionable message (Developer Console hint)",
      r.isError === true && text(r).includes("Developer Console"),
      text(r)
    );
    await badClient.close();

    /* ====================== no config at all =============================== */
    console.log("\n— no config");
    const env = { ...process.env };
    delete env.ACM_CONFIG;
    delete env.ACM_CONFIG_FILE;
    const noConfig = spawnSync(process.execPath, [SERVER_PATH], { env, encoding: "utf8", timeout: 20000 });
    check(
      "missing config exits with a clear message",
      (noConfig.status ?? 1) !== 0 && /ACM_CONFIG_FILE or ACM_CONFIG is required/.test(noConfig.stderr),
      noConfig.stderr.slice(0, 300)
    );
  } finally {
    for (const mock of mocks) mock.kill();
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
