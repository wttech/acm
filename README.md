[![WTT logo](docs/wtt-logo.png)](https://www.wundermanthompson.com)

<!--
[![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/wttech/acm)](https://github.com/wttech/acm/releases)
[![GitHub All Releases](https://img.shields.io/github/downloads/wttech/acm/total)](https://github.com/wttech/acm/releases)
[![Check](https://github.com/wttech/acm/workflows/Check/badge.svg)](https://github.com/wttech/acm/actions/workflows/check.yml)
-->
[![Apache License, Version 2.0, January 2004](docs/apache-license-badge.svg)](http://www.apache.org/licenses/)

<!--
<p>
  <img src="docs/logo-text.svg" alt="AEM Content Manager" width="300"/>
</p>
-->

**AEM Content Manager (ACM)**

Tool for doing live content and permission updates for AEM applications in a simple and flexible way.

Simply [install](#installation) ready-to-use CRX package on AEM instance and start migrating!

<p>
  <img src="docs/screenshot-console-code.png" alt="AEM Content Manager - Console Code (Screenshot)" width="640"/>
</p>

<p>
  <img src="docs/screenshot-console-output.png" alt="AEM Content Manager - Console Output (Screenshot)" width="640"/>
</p>

<p>
  <img src="docs/screenshot-executions.png" alt="AEM Content Manager - Executions  (Screenshot)" width="640"/>
</p>

<p>
  <img src="docs/screenshot-scripts-enabled.png" alt="AEM Content Manager - Scripts Enabled (Screenshot)" width="640"/>
</p>

<p>
  <img src="docs/screenshot-snippets.png" alt="AEM Content Manager - Snippets (Screenshot)" width="640"/>
</p>

---

Main concepts of AEM Content Manager tool are:

* **Simplicity**
    * Creating migration scripts should be as much simple as it is possible (no need to learn custom YML syntax or configuration language, just well-known Groovy),
    * No extra configuration needed - no hooks to needed to be configured in an AEM build (just provide Groovy scripts, the tool will do the rest),
* **Stability**
    * Executed always at optimal moment - safely use any classes from any OSGi bundles including your own,

## Installation

The ready-to-install AEM packages are available on [GitHub releases](https://github.com/wttech/acm/releases).

There are two ways to install AEM Content Manager on your AEM instances:

1. Using the 'all' package:
    * Recommended for fresh AEM instances.
    * This package will also install AEM Groovy Console and AEM Content Manager examples.
2. Using the 'minimal' package:
    * Recommended for AEM instances that already contain some dependencies shared with other tools.
    * This package does not include Groovy bundles, which can be provided by other tools like [AEM Easy Content Upgrade](https://github.com/valtech/aem-easy-content-upgrade/releases) (AECU) or [AEM Groovy Console](https://github.com/orbinson/aem-groovy-console/releases).

## Compatibility

| AEM Content Manager | AEM        | Java  | Groovy |
|---------------------|------------|-------|--------|
| 1.0.0               | 6.5, cloud | 8, 11 | 4.x    |

Note that AEM Content Manager is using Groovy scripts concept. However it is **not** using [AEM Groovy Console](https://github.com/icfnext/aem-groovy-console). It is done intentionally, because Groovy Console has close dependencies to concrete AEM version.
AEM Content Manager tool is implemented in a AEM version agnostic way, to make it more universal and more fault-tolerant when AEM version is changing.
It is compatible with AEM Groovy Console - simply install one of AEM Content Manager distributions without Groovy console OSGi bundle included as it is usually provided by Groovy Console AEM package.

## Documentation

### Basics

TODO

### OSGi configuration

TODO

## Other tools

TODO

## Authors

* [Krystian Panek](mailto:krystian.panek@vml.com) - Project Founder, Main Developer,
* [Dominik Przybył](mailto:dominik.przybyl@vml.com) - Developer,
* [Mariusz Pacyga](mailto:mariusz.pacyga@vml.com) - Developer.

## Contributing

Issues reported or pull requests created will be very appreciated.

1. Fork plugin source code using a dedicated GitHub button.
2. Do code changes on a feature branch created from *main* branch.
3. Create a pull request with a base of *main* branch.

## Building

To build all the modules run in the project root directory the following command with Maven 3:

    sh mvnw clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the following command:

    sh mvnw clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    sh mvnw clean install -PautoInstallSinglePackagePublish

Or alternatively

    sh mvnw clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    sh mvnw clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    sh mvnw clean install -PautoInstallPackage

## License

**AEM Content Manager** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
