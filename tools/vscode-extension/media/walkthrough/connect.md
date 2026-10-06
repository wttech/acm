# Connect to AEM

Instances live in settings, typically `.vscode/settings.json`, so a project can share them:

```json
"acm.instances": [
  { "name": "author", "url": "http://localhost:4502", "authMode": "basic", "user": "admin" },
  { "name": "dev", "url": "https://author-pXXXX-eYYYY.adobeaemcloud.com", "authMode": "bearer", "readonly": true }
]
```

| `authMode` | Secret |
|---|---|
| `basic` | password of `user` |
| `bearer` | access token, e.g. the AEMaaCS local development token |
| `cookie` | `login-token` cookie from a browser session |

Secrets are kept in VS Code secret storage, never in settings. The status bar shows the active instance; click it to switch.
