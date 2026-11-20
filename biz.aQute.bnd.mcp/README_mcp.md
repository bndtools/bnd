# bnd MCP Server

`biz.aQute.bnd.mcp` makes bnd usable for AI agents through the [Model Context Protocol](https://modelcontextprotocol.io):
create workspaces from templates, build, resolve, start `.bndrun` files and check them via HTTP.

Transport: **stdio** (JSON-RPC). Requirement: Java 17+.

## Documentation

Detailed description in the [doc/](./doc) directory:

| Document | Content |
|---|---|
| [doc/architecture.md](./doc/architecture.md) | goals, layers, design decisions, security model, lifecycle |
| [doc/implementation.md](./doc/implementation.md) | classes, flows, states, tests |
| [doc/tools.md](./doc/tools.md) | reference of all tools with parameters and results |

## Build

From the repository root:

```bash
./gradlew :biz.aQute.bnd.mcp:jar
./gradlew :biz.aQute.bnd.mcp:test
```

Result: `biz.aQute.bnd.mcp/generated/biz.aQute.bnd.mcp.jar` (executable, all dependencies embedded).

## Start

```bash
java -jar biz.aQute.bnd.mcp/generated/biz.aQute.bnd.mcp.jar
```

The server reads requests from stdin and writes responses to stdout; log output goes to stderr.
It terminates when the client closes stdin and stops all started runs in the process.

## Configure in an MCP client

VS Code (`.vscode/mcp.json`):

```json
{
  "servers": {
    "bnd": {
      "type": "stdio",
      "command": "java",
      "args": ["-jar", "${workspaceFolder}/biz.aQute.bnd.mcp/generated/biz.aQute.bnd.mcp.jar"]
    }
  }
}
```

Claude Desktop / other clients (`mcpServers`):

```json
{
  "mcpServers": {
    "bnd": {
      "command": "java",
      "args": ["-jar", "C:/path/to/biz.aQute.bnd.mcp.jar"]
    }
  }
}
```

## Tools

| Tool | Purpose |
|---|---|
| `bnd_fragments_list` | list template fragments |
| `bnd_workspace_create` | create/extend a workspace |
| `bnd_workspace_describe` | show projects, `.bndrun` files, repositories, errors |
| `bnd_build` | build a project |
| `bnd_resolve` | resolve a `.bndrun` |
| `bnd_run_start` | start a `.bndrun`, wait for readiness |
| `bnd_run_stop` | stop a run |
| `bnd_run_status` | state and console output |
| `bnd_http_get` | loopback HTTP check |

Details: [doc/tools.md](./doc/tools.md).

## Example flow

An agent could create and verify a workspace with a demo web app like this:

1. `bnd_fragments_list` – see the available fragments.
2. `bnd_workspace_create` with `{"dir": "C:/work/demo", "fragments": ["osgi", "demo-webapp"]}`.
3. `bnd_workspace_describe` with `{"dir": "C:/work/demo"}` – find projects and `.bndrun` files.
4. `bnd_build` with `{"workspaceDir": "C:/work/demo", "project": "<project>"}`.
5. `bnd_run_start` with `{"workspaceDir": "C:/work/demo", "bndrun": "<project>/<name>.bndrun"}` – returns `runId` and `httpPort`.
6. `bnd_http_get` with `{"url": "http://localhost:<httpPort>/", "expectStatus": 200}`.
7. `bnd_run_stop` with the `runId`.

## Error behavior

Business problems do not cause protocol errors but a tool result with `isError=true` and
`structuredContent.diagnostics.errors`. Warnings are in `diagnostics.warnings`.

## Security notes

- The server executes build and start code of the workspace; use it only with trusted workspaces.
- Fragments from other organisations are only installed with `trustThirdParty=true`.
- `.bndrun` files must be inside the workspace.
- `bnd_http_get` only allows loopback targets.
- No authentication: intended for local use over stdio.
