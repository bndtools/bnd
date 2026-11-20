# Tool Reference

The bnd MCP server provides nine tools. All return `structuredContent` (and the same content as JSON text in `content[0].text`).
If `diagnostics.errors` contains entries, `isError=true`.

Common result fragment:

```json
{ "diagnostics": { "errors": ["…"], "warnings": ["…"] } }
```

Required parameters are in **bold**. Unset numbers (`0`) mean "default".

## Overview

| Tool | Purpose |
|---|---|
| `bnd_fragments_list` | list template fragments from the indexes |
| `bnd_workspace_create` | create/extend a workspace |
| `bnd_workspace_describe` | describe a workspace |
| `bnd_build` | build a project |
| `bnd_resolve` | resolve a `.bndrun` |
| `bnd_run_start` | start a `.bndrun` and wait until ready |
| `bnd_run_stop` | stop a run |
| `bnd_run_status` | state and console of a run |
| `bnd_http_get` | loopback HTTP check |

Typical flow: `bnd_fragments_list` → `bnd_workspace_create` → `bnd_workspace_describe` → `bnd_build` → `bnd_resolve` → `bnd_run_start` → `bnd_http_get` → `bnd_run_stop`.

## `bnd_fragments_list`

Lists fragments (e.g. `gradle`, `osgi`, `demo-webapp`, Eclipse release repositories).

| Parameter | Type | Description |
|---|---|---|
| `indexes` | string[] | extra index URLs in addition to the bndtools default |
| `includeArchived` | boolean | include archived fragments (older Eclipse releases) |

Result: `fragments[] { name, id, description, official, archived, requires[] }`.
Unreadable indexes appear as a warning.

## `bnd_workspace_create`

Creates a workspace or extends it. Existing non-empty files stay unchanged and are reported in `keptFiles`.

| Parameter | Type | Description |
|---|---|---|
| **`dir`** | string | workspace directory; created if needed |
| `fragments` | string[] | fragments by name or ID; required fragments are added automatically |
| `indexes` | string[] | extra index URLs |
| `trustThirdParty` | boolean | allow fragments from other organisations (they can run code at build time) |

Result (`Info`): `dir`, `projects[]`, `bndruns[]`, `repositories[]`, `installedFragments[]`, `keptFiles[]`, `diagnostics`.

Typical errors: unknown fragment (with list of available ones), missing `trustThirdParty`, fragments with conflicting content for the same file.

## `bnd_workspace_describe`

| Parameter | Type | Description |
|---|---|---|
| **`dir`** | string | workspace directory |

Result (`Info`) as above, with `projects`, `bndruns` (relative, `/`-separated), `repositories` and the workspace errors/warnings. Error if `cnf/build.bnd` is missing.

## `bnd_build`

| Parameter | Type | Description |
|---|---|---|
| **`workspaceDir`** | string | workspace directory |
| **`project`** | string | project name |

Result: `project`, `outputs[]` (absolute paths of produced artifacts), `diagnostics` (build errors/warnings).

## `bnd_resolve`

| Parameter | Type | Description |
|---|---|---|
| **`workspaceDir`** | string | workspace directory |
| **`bndrun`** | string | `.bndrun`, workspace-relative or absolute; must be inside the workspace |
| `writeChanges` | boolean | write changed `-runbundles` back to the file |

Result: `bndrun` (resolved path), `runbundles` (resolution result), `diagnostics`.

## `bnd_run_start`

Starts the `.bndrun` in a child JVM. Resolves first, assigns a free HTTP port (`org.osgi.service.http.port`) and waits for readiness.

| Parameter | Type | Description |
|---|---|---|
| **`workspaceDir`** | string | workspace directory |
| **`bndrun`** | string | `.bndrun` file |
| `startupTimeoutMillis` | integer | time to wait for readiness, default 30000 |
| `readiness` | string | `http` (default): port accepts connections; `console`: console line matches `readyPattern`; `none`: process is running |
| `readyPattern` | string | Java regex, required for `readiness=console` |

Result (`RunResult`):

| Field | Description |
|---|---|
| `runId` | ID for `bnd_run_stop` / `bnd_run_status` |
| `state` | `STARTING`, `RUNNING`, `EXITED`, `FAILED`, `STOPPED`, `UNKNOWN` |
| `httpPort` | assigned HTTP port |
| `exitCode` | exit code; `null` while the run has not ended |
| `console` | last 200 output lines at most |
| `diagnostics` | errors, e.g. timeout or premature exit |

Example (result):

```json
{
  "runId": "5d0c…",
  "state": "RUNNING",
  "httpPort": 52431,
  "exitCode": null,
  "console": ["…"],
  "diagnostics": { "errors": [], "warnings": [] }
}
```

## `bnd_run_stop`

| Parameter | Type | Description |
|---|---|---|
| **`runId`** | string | run ID from `bnd_run_start` |

Idempotent: an unknown or already ended ID yields `STOPPED` without error.
If the run does not end within 5000 ms, `diagnostics` contains an error.

## `bnd_run_status`

| Parameter | Type | Description |
|---|---|---|
| **`runId`** | string | run ID from `bnd_run_start` |

Returns `RunResult` as above. An unknown ID yields state `UNKNOWN` and an error.

## `bnd_http_get`

GET against a loopback URL, retried until the expectation is met or the timeout elapses.

| Parameter | Type | Description |
|---|---|---|
| **`url`** | string | `http://` URL with loopback host (`localhost`, `127.0.0.1`, `[::1]`), without user info |
| `expectStatus` | integer | expected status, default 200 |
| `expectContains` | string | expected substring in the body |
| `timeoutMillis` | integer | total timeout for retries, default 5000 |

Result: `url`, `status`, `body`, `attempts`, `diagnostics`.

## Error behavior of the tools

| Case | `isError` | Content |
|---|---|---|
| Required parameter missing | true | text `Missing required parameter(s): dir` |
| Invalid argument type | true | text `Invalid arguments: …` |
| Business error | true | `structuredContent.diagnostics.errors` |
| Warnings only | false | `structuredContent.diagnostics.warnings` |
| Unexpected exception | true | text `Class: message` |
