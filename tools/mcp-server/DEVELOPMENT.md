# MCP server development

How to build, test and release the ACM MCP server. For using it, see the [README](README.md).

## Building and testing

```bash
cd tools/mcp-server
npm install
npm run check     # what CI runs: builds, then runs the end-to-end tests
```

`npm test` first runs the unit tests of [tools/shared](../shared) (`../shared/test/*.test.ts`, Node's built-in test runner, `npm run test:unit`; `register.mjs` lets Node resolve the sources' extensionless imports), then starts the built server over stdio against a mock ACM backend and calls every tool:

- `test/mock-acm.mjs` emulates the ACM servlets under `/apps/acm/api/*`: the response envelope, all three auth modes, Granite CSRF, and a QUEUED → RUNNING → SUCCEEDED lifecycle, so the polling loop is really exercised.
- `test/run-tests.mjs` drives the server with the MCP SDK client: handshake, tool listing and every tool call.

To try the server against a real instance, run `test/smoke.mjs`. It calls `acm_health` and runs one harmless `println`. It reads connection settings from a `.env` file, which is git-ignored. The server itself never reads `.env`.

```bash
cp .env.example .env     # set AEM_BASE_URL and ONE auth option
npm run smoke
```

To run a local build from an MCP client, point it at `node /path/to/acm/tools/mcp-server/dist/index.js` instead of `npx`.

API paths, environment variable names and the behaviour shared with the VS Code extension live in [tools/shared](../shared); see the [tools development guide](../DEVELOPMENT.md).

## Releasing

The server is versioned and released separately from ACM, with tags prefixed `mcp-server-v`.

From the repository root, run:

```shell
sh taskw release:mcp-server -- <version>
```

This bumps the version in `package.json` and `package-lock.json` (`npm version <version> --no-git-tag-version`) and both `version` fields of `server.json`, then commits, pushes, tags the commit `mcp-server-v<version>` and pushes the tag.

The [MCP Server workflow](https://github.com/wttech/acm/blob/main/.github/workflows/release.mcp-server.yml) runs the tests and checks that the tag matches all three files. It then publishes the package to npm with provenance and publishes `server.json` to the [MCP Registry](https://registry.modelcontextprotocol.io). If only the registry step fails, re-run the job. The npm step skips versions that are already published.

### One-time setup

npm trusted publishing can only be configured for a package that already exists, so the first version is published by hand:

1. Log in with an account that can publish to the `@wppes` scope, then run `npm ci && npm publish` in `tools/mcp-server/`.
2. On npmjs.com, open the package's settings and add a trusted publisher: GitHub Actions, organization `wttech`, repository `acm`, workflow `release.mcp-server.yml`.
3. Optionally, set publishing access to require two-factor authentication and disallow tokens, so only the workflow can publish.
4. Push the `mcp-server-v<version>` tag for that version. The workflow skips the npm step and publishes to the MCP Registry.
