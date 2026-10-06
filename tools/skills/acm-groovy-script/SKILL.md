---
name: acm-groovy-script
description: Write, review, validate and run Groovy scripts for AEM Content Manager (ACM) on AEM 6.5, AMS and AEM as a Cloud Service. Use for content migrations, bulk content updates, permission (ACL) setup, reports and data exports, scheduled maintenance and ad-hoc console code, and whenever ACM MCP tools (acm_validate_code, acm_run_code) are available.
---

# ACM Groovy scripts

[AEM Content Manager (ACM)](https://github.com/wttech/acm) runs Groovy code on AEM: ad-hoc in its console, or as scripts stored under `/conf/acm/settings/script` and run manually or automatically.
This skill covers writing those scripts and running them safely.

## Essentials

- **Never invent API.** Every call on an ACM variable (`repo`, `acl`, `inputs`, `outputs`, `conditions`, `context`, …) must exist in [the API reference](references/api.md), which is generated from the ACM source. Search it for the class you need instead of guessing.
- **Script shape.** `boolean canRun()` and `void doRun()` are required; `void describeRun()` declares inputs; `def scheduleRun()` schedules automatic scripts. Console code may be bare statements.
- **Document stored scripts.** Start every manual, automatic and extension script with a `/* */` block (not `/** */`) holding YAML frontmatter (`version`, `author`, `category`, `tags`, and `schedule` for scheduled scripts) and a Markdown description of what the script does and why, followed by a blank line. ACM shows it in the UI; Mermaid diagrams are rendered too.
- **Safe changes.** A script that changes content declares `inputs.bool('dryRun') { value = true; switcher() }` and wraps writes in `repo.dryRun(inputs.value('dryRun')) { … }`.
- **Abortable loops.** Call `context.checkAborted()` inside every loop over content.
- **Idempotent.** Re-running must be safe: use `repo` `ensure*`/`save*` methods, `acl` with `skipIfExists()`, and a fitting `conditions.*` in `canRun()`.
- **Logging.** `log.*` for anything worth keeping (start, summary, errors), `out.*` for console-only progress, `println` only for throwaway output.
- **Prefer ACM services** (`repo`, `acl`, `formatter`, `notifier`) over raw JCR, Sling or Jackrabbit APIs.
- **With ACM MCP tools:** validate with `acm_validate_code` before every run, run with `dryRun` left on first, read the output, and only then run for real. Use `history=false` for read-only iterations while developing.
- **Do exactly what was asked.** If paths, scope, run frequency or instance type are unclear, ask before writing code.

## Workflow

1. **Understand the task.** Identify what changes, where (paths, resource types), on which instances (author/publish), and how often (once, on deploy, on schedule, on demand).
2. **Pick the script type** (table below). Ask if it is not obvious.
3. **Write the script** from a skeleton below or a template in [patterns](references/patterns.md). Check every ACM call in [the API reference](references/api.md).
4. **Validate and try it** when ACM MCP tools are available: `acm_validate_code`, then `acm_run_code` with `dryRun` on, then inspect the console output and outputs.
5. **Deliver** the script with its file path and a short summary of what it does and how to run it.

## Script types

| Type | Location | Runs | Typical `canRun()` |
|---|---|---|---|
| Console | none, pasted into the ACM Console | on demand, as the current user | none needed |
| Manual | `/conf/acm/settings/script/manual/{project}/` | from the Scripts page, as the current user, usually with inputs | `conditions.always()` |
| Automatic | `/conf/acm/settings/script/automatic/{project}/` | on instance boot or by `scheduleRun()`, as a service or impersonated user | `conditions.changed()`, `conditions.once()` |
| Extension | `/conf/acm/settings/script/extension/{project}/main.groovy` | hooks: `prepareRun`, `completeRun` | none |
| Mock | `/conf/acm/settings/script/mock/{project}/` | on HTTP requests to `/mock/*`, when the mock filter is enabled | none (`request()` matches) |

In an AEM project, scripts live in a content package, e.g. `ui.content/src/main/content/jcr_root/conf/acm/settings/script/manual/acme/ACME-123_update-teasers.groovy`. Prefix file names with a ticket or ordering number.
See [scripts](references/scripts.md) for conditions, schedules, inputs, outputs, locking, documentation, extension and mock scripts, and snippets.

## Skeletons

Manual script that changes content:

```groovy
/*
---
version: '1.0'
author: jane.doe@acme.com
category: migration
tags: ['content']
---
Trims teaser titles under the given root path.
*/

void describeRun() {
    inputs.path('rootPath') { value = '/content/acme'; description = 'Root path to process' }
    inputs.bool('dryRun') { value = true; switcher(); description = 'Preview changes without saving' }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def rootPath = inputs.value('rootPath')
    def dryRun = inputs.value('dryRun')
    log.info "Teaser update started | root=${rootPath}, dryRun=${dryRun}"

    def updated = 0
    repo.dryRun(dryRun) {
        repo.get(rootPath).query('nt:unstructured', "n.[sling:resourceType] = 'acme/components/teaser'").forEach { teaser ->
            context.checkAborted()
            def title = teaser.property('jcr:title', String)
            if (title && title != title.trim()) {
                teaser.saveProperty('jcr:title', title.trim())
                updated++
            }
        }
    }

    log.info "Teaser update finished | updated=${updated}, dryRun=${dryRun}"
}
```

Automatic script that runs after each deployment that changed it:

```groovy
/*
---
version: '1.0'
author: jane.doe@acme.com
category: setup
tags: ['config']
---
Creates the folder structure the ACME project expects. Re-applied whenever this script changes.
*/

boolean canRun() {
    return conditions.changed() && conditions.isInstanceAuthor()
}

void doRun() {
    log.info "Setup started"
    // idempotent changes only
    log.info "Setup finished"
}
```

Console code can skip the methods entirely:

```groovy
println repo.get('/content/acme/en').property('jcr:content/jcr:title', String)
```

## Common mistakes

- `inputs.number(...)` does not exist: use `integerNumber`, `decimalNumber`, `integerRange` or `decimalRange`.
- `RepoResource` deletes properties with `deleteProperty(key)`, not `removeProperty`.
- `resource.query(nodeType, where)` takes a node type first and a JCR-SQL2 condition on `n` second. For any other query use `repo.queryRaw(sql)`.
- Queries and traversals return single-use `Stream`s. Iterate with `forEach { }`, or call `.toList()` to reuse the result.
- `acl` permissions are set in closures: `allow { path = '/content/acme'; permissions = ['jcr:read'] }`. Group members are added on the group: `group.addMember(user)`.
- `conditions.always()` in an automatic script runs it on every boot or schedule tick. Use it only when that is intended.

## References

- [API reference](references/api.md): every ACM variable, class and method available to scripts, generated from the ACM source. Search it before using any ACM method.
- [Scripts](references/scripts.md): script types, conditions, schedules, inputs, outputs, logging, aborting, locking, documentation, extension and mock scripts, snippets, notifications.
- [Patterns](references/patterns.md): complete templates for a minimal script, content migration, ACL setup, CSV reports, scheduled cleanup and HTTP mocks.
- [ACM documentation](https://github.com/wttech/acm#documentation) and [example scripts](https://github.com/wttech/acm/tree/main/ui.content.example/src/main/content/jcr_root/conf/acm/settings/script).
