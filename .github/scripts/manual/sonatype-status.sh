#!/usr/bin/env bash
# -------------------------------------------------------------------------
# sonatype-status.sh – Check Sonatype Central Portal deployment status
# and optionally clean up the local release directory.
#
# For release deployments: queries the Sonatype status API using the
# deployment ID stored by sonatype-upload.sh.
#
#
# Usage:
#   SONATYPE_BEARER=<token> ./.github/scripts/sonatype-status.sh [options] [<release-dir>]
#
# Options:
#   --deployment-id <id>                    Deployment ID (skips deployment ID file)
#   --status-url <url>                      Status API URL (release only)
#   --clean                                 Remove release-dir after successful check
#
# Environment:
#   SONATYPE_BEARER   – Bearer token for authentication (required)
#
# -------------------------------------------------------------------------
set -euo pipefail

# ---- defaults -----------------------------------------------------------
STATUS_URL="https://central.sonatype.com/api/v1/publisher/status"
CLEAN=false
RELEASE_DIR=""
DEPLOYMENT_ID=""
# -------------------------------------------------------------------------

usage() {
	cat <<-EOF
	Usage: $(basename "$0") [options] [<release-dir>]

	Check Sonatype deployment status and optionally clean up.

	Options:
	  --deployment-id <id>              Deployment ID (skips deployment ID file lookup)
	  --status-url <url>                Status API URL (default: Sonatype Central Portal)
	  --clean                           Remove release-dir after successful verification
	  -h, --help                        Show this help message

	Environment:
	  SONATYPE_BEARER   Bearer token for authentication (required)
	EOF
	exit "${1:-0}"
}

# ---- parse arguments ----------------------------------------------------
while [[ $# -gt 0 ]]; do
	case "$1" in
		--deployment-id) DEPLOYMENT_ID="$2"; shift 2 ;;
		--status-url)   STATUS_URL="$2"; shift 2 ;;
		--clean)        CLEAN=true; shift ;;
		-h|--help)      usage 0 ;;
		-*)             echo "Unknown option: $1" >&2; usage 1 ;;
		*)              RELEASE_DIR="$1"; shift ;;
	esac
done

if [[ -z "${DEPLOYMENT_ID}" && -z "${RELEASE_DIR}" ]]; then
	echo "Error: either --deployment-id or a release directory argument is required" >&2
	usage 1
fi

: "${SONATYPE_BEARER:?Error: SONATYPE_BEARER environment variable is not set}"

# ---- release status check -----------------------------------------------
DEPLOYMENTID_FILE=""
if [[ -n "${RELEASE_DIR}" ]]; then
	DEPLOYMENTID_FILE="${RELEASE_DIR%/}_DEPLOYMENTID.txt"
fi

if [[ -z "${DEPLOYMENT_ID}" ]]; then
	if [[ ! -f "${DEPLOYMENTID_FILE}" ]]; then
		echo "Error: Deployment ID file not found: ${DEPLOYMENTID_FILE}" >&2
		echo "Run sonatype-upload.sh first to create a deployment, or pass --deployment-id." >&2
		exit 1
	fi

	DEPLOYMENT_ID=$(cat "${DEPLOYMENTID_FILE}")

	if [[ -z "${DEPLOYMENT_ID}" ]]; then
		echo "Error: Deployment ID file is empty: ${DEPLOYMENTID_FILE}" >&2
		exit 1
	fi
fi

echo "Checking release deployment status ..."
echo "  Deployment ID: ${DEPLOYMENT_ID}"
echo "  Status URL: ${STATUS_URL}"

# --ssl-no-revoke: Windows schannel only; ignored elsewhere. Needed behind
# corporate TLS interception where revocation checks fail (CRYPT_E_NO_REVOCATION_CHECK).
STATUS_RESPONSE=$(curl -sS --ssl-no-revoke -X POST \
	-H "Authorization: Bearer ${SONATYPE_BEARER}" \
	"${STATUS_URL}?id=${DEPLOYMENT_ID}" 2>&1) || true

# Extract deploymentState from JSON response (portable, no grep -P)
STATE=$(echo "${STATUS_RESPONSE}" | sed -n 's/.*"deploymentState"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

echo "Deployment state: ${STATE:-UNKNOWN}"

case "${STATE}" in
	PUBLISHED)
		echo "Deployment successfully published."
		if [[ "${CLEAN}" == "true" && -n "${RELEASE_DIR}" ]]; then
			echo "Cleaning up release directory: ${RELEASE_DIR}"
			rm -rf "${RELEASE_DIR}"
			[[ -n "${DEPLOYMENTID_FILE}" ]] && rm -f "${DEPLOYMENTID_FILE}"
			echo "Release directory and deployment ID file removed."
		fi
		exit 0
		;;
	VALIDATED)
		echo "Deployment validated (awaiting manual publishing)."
		exit 0
		;;
	PENDING|VALIDATING|PUBLISHING)
		echo "Deployment is still in progress (${STATE})."
		exit 2
		;;
	FAILED)
		echo "Error: Deployment failed." >&2
		echo "${STATUS_RESPONSE}" >&2
		exit 1
		;;
	*)
		echo "Unknown deployment state: ${STATE:-UNKNOWN}" >&2
		echo "${STATUS_RESPONSE}" >&2
		exit 1
		;;
esac
