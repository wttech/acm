# Let an agent help

The extension gives Copilot:

- the **ACM scripting skill**: the real ACM API, dry runs, abort checks and documentation headers;
- the **ACM MCP server** for the active instance: validate and run scripts, follow executions, read outputs.

Try in agent mode:

> Count the teaser components under /content/wknd/us/en using ACM.

The agent acts with your AEM permissions and every run is traceable in ACM. Review tool calls before approving them, and prefer `readonly` instances outside local development.

Using another tool, such as Claude Code, Cursor or Devin? Run `ACM: Copy MCP Setup Prompt` and paste it into that agent.
