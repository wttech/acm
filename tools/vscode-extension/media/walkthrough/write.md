# Write a script

Every stored script starts with its documentation, shown in the ACM UI:

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
    inputs.path('rootPath') { value = '/content/acme' }
    inputs.bool('dryRun') { value = true; switcher() }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    repo.dryRun(inputs.value('dryRun')) {
        // changes here are reverted while dryRun is on
    }
}
```

Type `acmdoc` to add the documentation header to an existing script.
