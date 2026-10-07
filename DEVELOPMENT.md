# Development

How to build, run and release ACM itself. The tools (MCP server, VS Code extension) are covered in [tools/DEVELOPMENT.md](tools/DEVELOPMENT.md).

## Building

```shell
sh mvnw clean install
```

The build formats Java code with Spotless (Palantir Java Format). ACM must work the same on AEM 6.5 on-premise, AMS and AEM as a Cloud Service, on Java 8, 11 and 21, so use only Java 8 language features and APIs.

## Local development

1. All-in-one command (incremental building and deployment of 'all' distribution, both backend & frontend)

    ```shell
    sh taskw develop:all
    ```

2. Example contents

    ```shell
    sh taskw develop:content:example
    ```

3. Backend only

    ```shell
    sh taskw develop:core
    ```

4. Frontend only with production build mode

    ```shell
    sh taskw develop:frontend
    ```

5. Frontend only with dev build mode (live reloading)

    ```shell
    sh taskw develop:frontend:dev
    ```

## Releasing

1. To check the last release version, run:

    ```shell
    sh taskw release
    ```

2. To release a new version, run:

    ```shell
    sh taskw release -- <new-version>
    ```
