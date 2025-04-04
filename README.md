<a href="https://www.vml.com/expertise/enterprise-solutions" target="_blank">
  <picture>
    <source srcset="docs/vml-logo-white.svg" media="(prefers-color-scheme: dark)">
    <img src="docs/vml-logo-black.svg" alt="VML Logo" height="100">
  </picture>
</a>

[![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/wttech/acm)](https://github.com/wttech/acm/releases)
[![GitHub All Releases](https://img.shields.io/github/downloads/wttech/acm/total)](https://github.com/wttech/acm/releases)
[![Check](https://github.com/wttech/acm/workflows/Check/badge.svg)](https://github.com/wttech/acm/actions/workflows/check.yml)
[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)

# AEM Content Manager (ACM)

ACM is a powerful tool designed to streamline your workflow and enhance productivity with an intuitive interface and robust features.

## Key Features

### All-in-one Solution
ACM is a comprehensive alternative to tools like APM, AECU, AEM Groovy Console, and AC Tool. It leverages the Groovy language, which is familiar to most Java developers, eliminating the need to learn custom YAML syntax or languages/grammars. Enjoy a single, painless tool setup in AEM projects with no hooks and POM updates.

### New Approach
Experience a different way of using Groovy scripts. ACM ensures the instance is healthy before scripts decide when to run: once, periodically, or at an exact date and time. Execute scripts in parallel or sequentially, offering unmatched flexibility and control.

### Content Management
Effortlessly migrate pages and components between versions. Ensure content integrity and resolve issues with confidence.

### Permissions Management
Apply JCR permissions dynamically. Manage permissions seamlessly during site creation, blueprinting, and for live copies, language copies, and other AEM-specific replication scenarios.

### Data Imports & Exports
Effortlessly integrate data from external sources into the JCR repository, enhancing content management capabilities. By simplifying data import implementation, ACM allows developers to focus more on developing better components and presenting data effectively, ensuring a user-friendly experience.

## Installation

The ready-to-install AEM packages are available on [GitHub releases](https://github.com/wttech/acm/releases).

There are two ways to install AEM Content Manager on your AEM instances:

1. **Using the 'all' package:**
    * Recommended for fresh AEM instances.
    * This package will also install AEM Groovy Console and AEM Content Manager examples.
2. **Using the 'minimal' package:**
    * Recommended for AEM instances that already contain some dependencies shared with other tools.
    * This package does not include Groovy bundles, which can be provided by other tools like [AEM Easy Content Upgrade](https://github.com/valtech/aem-easy-content-upgrade/releases) (AECU) or [AEM Groovy Console](https://github.com/orbinson/aem-groovy-console/releases).

## Compatibility

| AEM Content Manager | AEM        | Java  | Groovy |
|---------------------|------------|-------|--------|
| 1.0.0               | 6.5, cloud | 8, 11 | 4.x    |

## Documentation

### Basics

**Groovy code can be run in three ways:**

1. **Ad-hoc using 'Console'**
   - Code executed in the console is run in the context of the currently logged user to AEM.

2. **Manually executed scripts**
   - Navigate to the 'Scripts' page and select the 'Manual' tab.
   - Code executed here runs in the context of the system user or an impersonated user when configured additionally.

3. **Automatically executed scripts**
   - Navigate to the 'Scripts' page and select the 'Automatic' tab.
   - Code can be scheduled to run once, periodically, or at an exact date and time.

**Rules for executing Groovy code:**

- **Context**: Code can leverage any Java code deployed in the AEM instance as OSGi bundles, including project code.
- **Health Checks**: ACM performs health checks to ensure the instance is stable before executing scripts. These checks include:
  - OSGi bundles (with the ability to exclude some to address known issues)
  - OSGi events occurrence indicating temporal instability
  - JCR repository paths presence (e.g., `/content/acme`, `/content/dam/acme`)

### Extension scripts

To add own code binding or hook into execution process, you can create your own extension Groovy scripts and place them at path like `/conf/acm/settings/extension/acme/main.groovy`.

**Example of an extension script:**

```groovy
void extend(Extender extender) {
  extender.codeBindingVariable("acme", AcmeApi.class) { new AcmApiImpl(executionContext.resourceResolver)) }
  // or just
  extender.codeBindingVariable("acme") { new AcmApiImpl(executionContext.resourceResolver) }
}

void completeExecution(Execution execution) {
   if (execution.status === 'FAILED') {
      // send Slack/MS Teams message or something
   }
}
```

## Development

1. All-in-one command (incremental building and deployment of 'all' distribution, both backend & frontend)

    ```shell
    sh taskw develop:all
    ```

2. Example contents

    ```shell
    sh taskw develop:content:example
    ```

3. Frontend with live reloading:

    ```shell
    sh taskw develop:frontend:dev
    ```

## License

**AEM Content Manager** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)