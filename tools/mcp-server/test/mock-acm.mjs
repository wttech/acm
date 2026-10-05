#!/usr/bin/env node
/**
 * Mock ACM/AEM backend for testing the ACM MCP server.
 *
 * Emulates the ACM servlet endpoints under /apps/acm/api/* with the
 * { status, message, data } envelope, plus AEM's Granite CSRF endpoint.
 *
 * Auth accepted:
 *   - Basic admin:admin
 *   - Cookie "login-token=mock-login-token" (non-GET additionally requires
 *     header CSRF-Token: mock-csrf-token, like real AEM)
 *   - Bearer "mock-bearer-token"
 *
 * Execution lifecycle: queued code starts QUEUED, advances one state per
 * poll (QUEUED -> RUNNING -> SUCCEEDED) so the MCP server's polling loop is
 * actually exercised. Code containing "NOT_GROOVY" fails compilation; code
 * containing "FAIL_AT_RUNTIME" ends FAILED; "LOCKED_RUN" ends LOCKED;
 * "RUN_FOREVER" stays RUNNING until aborted. A login-token containing
 * "slow-csrf" makes the CSRF endpoint hang.
 */
import http from "node:http";

const PORT = Number(process.env.PORT || 45102);

const executions = new Map(); // id -> { exec, pollsLeft, forever }
let execCounter = 0;

const SCRIPTS = [
  {
    id: "/conf/acm/settings/script/manual/example/hello.groovy",
    path: "/conf/acm/settings/script/manual/example/hello.groovy",
    content:
      'boolean canRun() {\n    return conditions.always()\n}\n\nvoid doRun() {\n    println "hello from stored script"\n}',
  },
  {
    id: "/conf/acm/settings/script/manual/example/cleanup.groovy",
    path: "/conf/acm/settings/script/manual/example/cleanup.groovy",
    content:
      'boolean canRun() {\n    return conditions.always()\n}\n\nvoid doRun() {\n    repo.dryRun(true) {\n        out.info("would clean")\n    }\n}',
  },
];

function envelope(res, httpStatus, message, data) {
  res.writeHead(httpStatus, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ status: httpStatus, message, data }));
}

function checkAuth(req, res) {
  const auth = req.headers["authorization"] || "";
  const cookie = req.headers["cookie"] || "";
  const basicOk = auth === "Basic " + Buffer.from("admin:admin").toString("base64");
  const bearerOk = auth === "Bearer mock-bearer-token";
  const cookieOk = cookie.includes("login-token=mock-login-token");
  if (basicOk || bearerOk) return { ok: true, mode: "header" };
  if (cookieOk) return { ok: true, mode: "cookie" };
  res.writeHead(401, { "Content-Type": "text/plain" });
  res.end("401 Unauthorized (mock)");
  return { ok: false };
}

function newExecution(content) {
  execCounter += 1;
  const id = `2026/06/10/mock_execution_${String(execCounter).padStart(3, "0")}`;
  const forever = content.includes("RUN_FOREVER");
  const willFail = content.includes("FAIL_AT_RUNTIME");
  const willLock = content.includes("LOCKED_RUN");
  const exec = {
    id,
    userId: "admin",
    status: "QUEUED",
    startDate: new Date().toISOString(),
    executable: { id: "console", content },
  };
  executions.set(id, {
    exec,
    pollsLeft: 2, // QUEUED -> RUNNING -> terminal
    forever,
    willFail,
    willLock,
    consoleOutput: null,
  });
  return exec;
}

function advance(entry) {
  const { exec } = entry;
  if (["SUCCEEDED", "FAILED", "ABORTED", "SKIPPED", "LOCKED"].includes(exec.status)) return;
  if (entry.forever) {
    exec.status = "RUNNING";
    return;
  }
  entry.pollsLeft -= 1;
  if (entry.pollsLeft === 1) {
    exec.status = "RUNNING";
  } else if (entry.pollsLeft <= 0) {
    exec.endDate = new Date().toISOString();
    exec.duration = 1234;
    if (entry.willLock) {
      exec.status = "LOCKED";
      entry.consoleOutput = "";
    } else if (entry.willFail) {
      exec.status = "FAILED";
      exec.error = "java.lang.IllegalStateException: boom (mock runtime failure)";
      entry.consoleOutput = "about to boom...\n";
    } else {
      exec.status = "SUCCEEDED";
      exec.error = null;
      entry.consoleOutput = "hello from mock ACM\n42\n";
    }
  }
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const path = url.pathname;

  // CSRF token endpoint — requires auth but never CSRF itself
  if (path === "/libs/granite/csrf/token.json") {
    if (!checkAuth(req, res).ok) return;
    if ((req.headers["cookie"] || "").includes("slow-csrf")) return; // never answers
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ token: "mock-csrf-token" }));
    return;
  }

  const auth = checkAuth(req, res);
  if (!auth.ok) return;

  // Real AEM enforces CSRF for cookie-authenticated mutations
  if (auth.mode === "cookie" && req.method !== "GET") {
    if (req.headers["csrf-token"] !== "mock-csrf-token") {
      res.writeHead(403, { "Content-Type": "text/plain" });
      res.end("403 Forbidden: missing/invalid CSRF token (mock)");
      return;
    }
  }

  let body = "";
  for await (const chunk of req) body += chunk;
  const json = body ? JSON.parse(body) : undefined;

  // --- state ---------------------------------------------------------------
  if (req.method === "GET" && path === "/apps/acm/api/state.json") {
    return envelope(res, 200, "State read successfully", {
      healthStatus: { healthy: true, issues: [] },
      queuedExecutions: [...executions.values()]
        .filter((e) => ["QUEUED", "RUNNING"].includes(e.exec.status))
        .map((e) => e.exec.id),
      instanceSettings: { id: "mock-aem", timezone: "UTC" },
    });
  }

  // --- execute-code (parse/validate, or synchronous run) --------------------
  if (req.method === "POST" && path === "/apps/acm/api/execute-code.json") {
    const content = json?.code?.content || "";
    if (json?.mode === "RUN") {
      // Mirrors ExecuteCodeServlet: runs inline, and history=false keeps the
      // execution out of /apps/acm/api/execution.json.
      if (content.includes("RUN_FOREVER")) return; // never answers, like a stuck script
      const exec = newExecution(content);
      const entry = executions.get(exec.id);
      entry.pollsLeft = 0;
      advance(entry);
      exec.output = entry.consoleOutput;
      if (json.history === false) executions.delete(exec.id);
      return envelope(res, 200, "Code executed", exec);
    }
    if (json?.mode !== "parse") {
      return envelope(res, 400, "Mock only supports mode=parse or mode=RUN here", null);
    }
    if (content.includes("NOT_GROOVY")) {
      return envelope(res, 200, "Code parsed", {
        id: "parse-only",
        status: "FAILED",
        error:
          "startup failed:\nconsole.groovy: 6: unexpected token: NOT_GROOVY @ line 6, column 5.\n1 error",
      });
    }
    return envelope(res, 200, "Code parsed", { id: "parse-only", status: "SUCCEEDED", error: null });
  }

  // --- queue-code: POST=queue, GET=poll, DELETE=abort ------------------------
  if (path === "/apps/acm/api/queue-code.json") {
    if (req.method === "POST") {
      const content = json?.code?.content || "";
      const exec = newExecution(content);
      return envelope(res, 200, "Code queued", { executions: [exec] });
    }
    if (req.method === "GET") {
      const id = url.searchParams.get("executionId");
      const entry = executions.get(id);
      if (!entry) return envelope(res, 200, "Queue read", { executions: [] });
      advance(entry);
      return envelope(res, 200, "Queue read", { executions: [entry.exec] });
    }
    if (req.method === "DELETE") {
      const id = url.searchParams.get("executionId");
      const entry = executions.get(id);
      if (entry && ["QUEUED", "RUNNING"].includes(entry.exec.status)) {
        entry.exec.status = "ABORTED";
        entry.exec.endDate = new Date().toISOString();
        entry.consoleOutput = "(aborted)\n";
        return envelope(res, 200, "Execution aborted", {});
      }
      return envelope(res, 404, "Execution not found or not running", null);
    }
  }

  // --- execution history ------------------------------------------------------
  if (req.method === "GET" && path === "/apps/acm/api/execution.json") {
    const id = url.searchParams.get("id");
    let list = [...executions.values()].map((e) => e.exec);
    if (id) list = list.filter((e) => e.id === id);
    else list = list.filter((e) => !["QUEUED", "RUNNING"].includes(e.status));
    const limit = Number(url.searchParams.get("limit") || 20);
    return envelope(res, 200, "Executions read", { list: list.slice(0, limit) });
  }

  // --- execution output --------------------------------------------------------
  if (req.method === "GET" && path === "/apps/acm/api/execution-output.json") {
    const id = url.searchParams.get("executionId");
    const name = url.searchParams.get("name");
    const entry = executions.get(id);
    if (!entry || entry.consoleOutput === null) {
      res.writeHead(404, { "Content-Type": "text/plain" });
      res.end("Output not found (mock)");
      return;
    }
    if (name === "console") {
      res.writeHead(200, { "Content-Type": "text/plain" });
      res.end(entry.consoleOutput);
      return;
    }
    if (name === "report") {
      res.writeHead(200, { "Content-Type": "text/csv" });
      res.end("path,action\n/content/foo,deleted\n");
      return;
    }
    res.writeHead(404, { "Content-Type": "text/plain" });
    res.end("Output not found (mock)");
    return;
  }

  // --- scripts ------------------------------------------------------------------
  if (req.method === "GET" && path === "/apps/acm/api/script.json") {
    const id = url.searchParams.get("id");
    if (id) {
      const script = SCRIPTS.filter((s) => s.id === id);
      return envelope(res, 200, "Scripts read", { list: script });
    }
    const type = url.searchParams.get("type") || "MANUAL";
    return envelope(res, 200, "Scripts read", {
      list: type === "MANUAL" ? SCRIPTS.map(({ id, path }) => ({ id, path })) : [],
    });
  }

  // --- describe-code ---------------------------------------------------------------
  if (req.method === "POST" && path === "/apps/acm/api/describe-code.json") {
    return envelope(res, 200, "Code described", {
      execution: { id: "describe-only", status: "SUCCEEDED" },
      description: {
        arguments: {
          dryRun: { name: "dryRun", type: "BOOL", value: true, label: "Dry run" },
        },
      },
    });
  }

  res.writeHead(404, { "Content-Type": "text/plain" });
  res.end(`404 mock: no route for ${req.method} ${path}`);
});

server.listen(PORT, () => {
  console.log(`[mock-acm] listening on http://localhost:${PORT}`);
});
