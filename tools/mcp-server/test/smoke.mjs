#!/usr/bin/env node
/**
 * Live smoke test against a real AEM/ACM instance, configured via .env.
 * .env sets ACM_CONFIG_FILE (path to the config document) or ACM_CONFIG
 * (the document inline). Runs acm_list_instances, acm_health and one
 * harmless println via acm_run_code.
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { readFileSync } from "node:fs";

const envFile = new URL("../.env", import.meta.url).pathname;
const env = {};
for (const line of readFileSync(envFile, "utf8").split("\n")) {
  const m = line.match(/^\s*([A-Z_]+)\s*=\s*(.*)\s*$/);
  if (m && m[2]) env[m[1]] = m[2];
}
if (!env.ACM_CONFIG && !env.ACM_CONFIG_FILE) {
  console.error("ACM_CONFIG or ACM_CONFIG_FILE missing in .env");
  process.exit(1);
}

const transport = new StdioClientTransport({
  command: process.execPath,
  args: [new URL("../dist/index.js", import.meta.url).pathname],
  env: { ...process.env, ...env },
  stderr: "pipe",
});
const client = new Client({ name: "acm-smoke", version: "1.0.0" });
await client.connect(transport);
console.log("MCP handshake OK\n");

const text = (r) => (r.content || []).map((c) => c.text ?? "").join("\n");

console.log("=== acm_list_instances ===");
let r = await client.callTool({ name: "acm_list_instances", arguments: {} });
console.log(`isError: ${!!r.isError}`);
console.log(text(r).slice(0, 1500));

console.log("\n=== acm_health ===");
r = await client.callTool({ name: "acm_health", arguments: {} });
console.log(`isError: ${!!r.isError}`);
console.log(text(r).slice(0, 1500));

if (!r.isError) {
  console.log("\n=== acm_run_code (harmless println) ===");
  r = await client.callTool({
    name: "acm_run_code",
    arguments: { code: 'println "ACM MCP smoke test OK: " + new Date()' },
  });
  console.log(`isError: ${!!r.isError}`);
  console.log(text(r).slice(0, 1500));
}

await client.close();
process.exit(0);
