# Changelog

## 0.1.2

- Multi-line text inputs (`TEXT`) are edited in an editor and confirmed with a picker instead of a single-line input box that dropped line breaks.
- Executions view can be filtered by script and status; the Instance Scripts view has a button to show a script's executions.
- Manual scripts have a Run button in the Instance Scripts view; stored scripts (opened from the views) run by ID, so their executions are recorded under the script instead of `console`. Compare with Local File moved to the context menu.
- Views are ordered Project Scripts, Instance Scripts, Executions. Instance Scripts shows scripts like the ACM UI: flat, named by their path under the type folder, groups expanded, empty types hidden, with the instance name as the view description.
- New *Project Scripts* view for the scripts of the project's content package, shown when the folder is found (`acm.scripts.root` or discovery of `jcr_root/conf/acm/settings/script`). Right-click a type to create a script from a template; run manual scripts (as console code), compare with the instance version, duplicate, rename, delete and reveal in Explorer from the view.
- Failures of view commands (filtering) are shown as errors instead of being lost.
- Status bar names the issues ACM's health check found on an unhealthy instance (they were never listed) and warns that running scripts may be unsafe; the MCP `acm_health` tool warns the same way.
- README and Marketplace description lead with the automatically registered MCP server and the setup prompt for other AI tools.
- Execution logs have their own syntax highlighting (header, sections, log levels, quoted values) instead of the generic log one that mangled the execution ID and executable path.

## 0.1.0

First preview.

- Instances in settings with a zero-config default for a local AEM SDK (`author`, `http://localhost:4502`); credentials in secret storage; read-only and non-local guardrails.
- Run scripts or selections with inputs (including file uploads), live console output and abort; run without history for users allowed to; compile-check on save.
- Executions and Scripts views, read-only execution logs (with inputs) and stored scripts, compare with local files, download of outputs.
- Instance health in the status bar; expired credentials lead to `ACM: Set Credentials`.
- Bundled ACM MCP server offered to Copilot agent mode for the active instance; `ACM: Copy MCP Setup Prompt` for other AI tools.
- *Get Started with ACM* walkthrough.
- Requires VS Code 1.101 or later.
- Completion and hover for the ACM script API; `New ACM Script` templates and the `acmdoc` documentation snippet.
- ACM Groovy scripting skill for Copilot.
