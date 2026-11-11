# Manual Release and Integration Scripts

These scripts support manual release, repository-permission, and Eclipse
product-installation checks. They are not CI workflow entry points. Run them
from a Bash-compatible shell and supply credentials only through environment
variables; do not put tokens on command lines or in files.

| Script | Purpose | External services |
| --- | --- | --- |
| [`install-eclipse-product.sh`](install-eclipse-product.sh) | Install an Eclipse IDE plus Bndtools P2 features into an isolated runtime and verify headless launch. | Eclipse downloads, SimRel, Bndtools JFrog P2 repository |
| [`sonatype-status.sh`](sonatype-status.sh) | Query a Central Portal deployment and optionally remove a successful local release directory. | Sonatype Central Portal |
| [`test-jfrog-snapshot-token.sh`](test-jfrog-snapshot-token.sh) | Check that a snapshot P2 publishing token has only required repository rights. | Bndtools JFrog Artifactory |

## `install-eclipse-product.sh`

Builds a temporary Eclipse product installation from P2 repositories. It
downloads and caches an Equinox Starter Kit, starts the P2 director, installs
the Eclipse Platform IDE and Bndtools feature groups, then runs a headless
Ant application to verify that the installed product launches.

The default Bndtools repository is pull-request repository `p2/pr/7430`.
Override `PULLREQUESTS` or `BNDTOOLS_REPOSITORY` when validating another pull
request or repository. The script detects host OS, window system, and CPU
architecture, although `TARGET_OS`, `TARGET_WS`, and `TARGET_ARCH` can
override detection.

### Prerequisites

- Bash, `curl`, `tar` or `unzip`, `find`, `sed`, `grep`, and standard Unix
  utilities.
- `JAVA_HOME` points to an executable JDK 21 or newer. Set
  `JAVA_MIN_VERSION` only when a different minimum is intended.
- Network access to Eclipse download sites and the selected Bndtools P2
  repository.

### Example

```bash
cd .github/scripts/manual
JAVA_HOME=/path/to/jdk-21 \
PULLREQUESTS=7430 \
./install-eclipse-product.sh
```

The default output is a timestamped directory under `runtime/` in the current
directory. It contains `instance/` (installed product), `data/` (headless
workspace), and `p2-director.log`. Reuse downloaded Starter Kits through
`CACHE_DIR`; relocate all run output through `RUNTIME_ROOT` or `RUN_DIR`.

### Key configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `EQUINOX_DROP_URL` | Eclipse 4.41 Equinox drop | Starter Kit and P2-director bundle source |
| `STARTER_KIT_VERSION` | `4.41` | Starter Kit release family |
| `PULLREQUESTS` | `7430` | Pull request used to construct Bndtools P2 URL |
| `BNDTOOLS_REPOSITORY` | JFrog P2 PR URL | Bndtools update site |
| `SIMREL_REPOSITORY`, `ECLIPSE_REPOSITORY` | Eclipse 2026-09, 4.41 | Eclipse installation repositories |
| `INSTALL_IUS` | IDE plus three Bndtools feature groups | Installable units passed to P2 director |
| `DESTINATION`, `DATA_DIR`, `LOG_FILE` | under timestamped run directory | Product, workspace, and log paths |
| `PROGRESS_INTERVAL` | `15` seconds | Positive interval for P2-director progress output |

The script creates and later removes a temporary director configuration and
debug options file. It does not remove the installed product or cache after a
successful run. On failure, inspect `p2-director.log` and any workspace log at
`data/.metadata/.log`.

## `sonatype-status.sh`

Queries deployment status from Sonatype Central Portal. Supply either a
deployment ID directly or a release directory whose sibling deployment-ID file
was created by `sonatype-upload.sh`:

```bash
SONATYPE_BEARER=<token> \
  ./.github/scripts/manual/sonatype-status.sh path/to/release

SONATYPE_BEARER=<token> \
  ./.github/scripts/manual/sonatype-status.sh --deployment-id <id>
```

`SONATYPE_BEARER` is required. The default endpoint is
`https://central.sonatype.com/api/v1/publisher/status`; use `--status-url` only
for an intentional endpoint override.

Exit status is meaningful for automation:

| Result | Exit status |
| --- | --- |
| `PUBLISHED` or `VALIDATED` | `0` |
| `PENDING`, `VALIDATING`, or `PUBLISHING` | `2` |
| Failed, unknown, missing input, or missing token | `1` |

`--clean` removes the supplied release directory and its
`_DEPLOYMENTID.txt` file only after `PUBLISHED`. Do not use it for a deployment
that needs retained release artifacts.

The script uses `curl --ssl-no-revoke` to accommodate Windows corporate TLS
interception. It prints full API responses only for failed or unknown states,
so avoid forwarding its output when deployment metadata is sensitive.

## `test-jfrog-snapshot-token.sh`

Performs a disposable Artifactory permission probe for publication to
`p2/pr/**`. It creates a randomized probe object, verifies write, read, and
delete rights, checks that writes to `libs-snapshot-local` and
`libs-release-local` are denied, and checks that deletion outside `p2/pr/**`
is denied. Cleanup attempts to delete the probe object even when a check
fails.

```bash
JFROG_SNAPSHOT_TOKEN=<token> \
  ./.github/scripts/manual/test-jfrog-snapshot-token.sh
```

Requirements: Bash and `curl`; `JFROG_SNAPSHOT_TOKEN` is mandatory.
`JFROG_URL` defaults to `https://bndtools.jfrog.io` and can target an intended
test server. For corporate TLS interception, set
`CURL_EXTRA_OPTS=--ssl-no-revoke`.

Each required right must return its expected HTTP status: PUT `201`, HEAD
`200`, and DELETE `200` or `204`. Restricted paths must return `403` or `404`.
Any failed assertion returns exit status `1`; an invalid or expired token
(`401`) stops immediately. The script never prints the token, but it performs
real remote writes and deletes under its unique probe path.

## Operational Notes

- Scripts use `set -euo pipefail`; unset variables and failed required steps
  stop execution.
- Run `bash -n .github/scripts/manual/*.sh` for a no-network syntax check.
- Treat `--clean` and JFrog probe execution as remote side effects. Confirm
  target URLs and selected paths before running them.
