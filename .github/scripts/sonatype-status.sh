#!/usr/bin/env bash
# -------------------------------------------------------------------------
# sonatype-status.sh – Check Sonatype Central Portal deployment status
# and optionally clean up the local release directory.
#
# For release deployments: queries the Sonatype status API using the
# deployment ID stored by sonatype-upload.sh.
#
# For snapshot deployments: verifies that ALL jar files from the
# snapshot deployment folder are accessible via the Sonatype snapshot
# repository URL.
#
# Usage:
#   SONATYPE_BEARER=<token> ./.github/scripts/sonatype-status.sh [options] <release-dir>
#
# Options:
#   --snapshot                              Check snapshot deployment
#   --status-url <url>                      Status API URL (release only)
#   --snapshot-url <url>                    Snapshot repository URL
#   --clean                                 Remove release-dir after successful check
#
# Environment:
#   SONATYPE_BEARER   – Bearer token for authentication (required)
#
# -------------------------------------------------------------------------
set -euo pipefail

# ---- defaults -----------------------------------------------------------
STATUS_URL="https://central.sonatype.com/api/v1/publisher/status"
SNAPSHOT_URL="https://central.sonatype.com/repository/maven-snapshots/"
SNAPSHOT=false
CLEAN=false
RELEASE_DIR=""
# -------------------------------------------------------------------------

usage() {
	cat <<-EOF
	Usage: $(basename "$0") [options] <release-dir>

	Check Sonatype deployment status and optionally clean up.

	Options:
	  --snapshot                        Check snapshot deployment
	  --status-url <url>                Status API URL (default: Sonatype Central Portal)
	  --snapshot-url <url>              Snapshot repository URL
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
		--snapshot)     SNAPSHOT=true; shift ;;
		--status-url)   STATUS_URL="$2"; shift 2 ;;
		--snapshot-url) SNAPSHOT_URL="$2"; shift 2 ;;
		--clean)        CLEAN=true; shift ;;
		-h|--help)      usage 0 ;;
		-*)             echo "Unknown option: $1" >&2; usage 1 ;;
		*)              RELEASE_DIR="$1"; shift ;;
	esac
done

if [[ -z "${RELEASE_DIR}" ]]; then
	echo "Error: release directory argument is required" >&2
	usage 1
fi

: "${SONATYPE_BEARER:?Error: SONATYPE_BEARER environment variable is not set}"

# ---- snapshot status check ----------------------------------------------
if [[ "${SNAPSHOT}" == "true" ]]; then
	echo "Checking snapshot deployment status ..."

	SNAPSHOT_BASE="${SNAPSHOT_URL%/}/"

	# Find all jar files in the snapshot deployment folder
	JARS=$(find "${RELEASE_DIR}" -name '*.jar' -type f)

	if [[ -z "${JARS}" ]]; then
		echo "Error: No jar files found in ${RELEASE_DIR} to verify" >&2
		exit 1
	fi

	TOTAL=0
	AVAILABLE=0
	MISSING=0

	while IFS= read -r jar_file; do
		# Convert to relative path
		REL_PATH="${jar_file#"${RELEASE_DIR}"}"
		REL_PATH="${REL_PATH#/}"

		CHECK_URL="${SNAPSHOT_BASE}${REL_PATH}"
		((TOTAL++)) || true

		HTTP_CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
			-H "Authorization: Bearer ${SONATYPE_BEARER}" \
			"${CHECK_URL}") || true

		if [[ "${HTTP_CODE}" -eq 200 ]]; then
			echo "  OK   ${REL_PATH}"
			((AVAILABLE++)) || true
		else
			echo "  MISS ${REL_PATH} (HTTP ${HTTP_CODE})"
			((MISSING++)) || true
		fi
	done <<< "${JARS}"

	echo "Snapshot verification: ${AVAILABLE}/${TOTAL} artifacts available, ${MISSING} missing."

	if [[ "${MISSING}" -gt 0 ]]; then
		echo "Error: Not all snapshot artifacts are available yet." >&2
		exit 1
	fi

	echo "All snapshot artifacts are available."
	if [[ "${CLEAN}" == "true" ]]; then
		echo "Cleaning up release directory: ${RELEASE_DIR}"
		rm -rf "${RELEASE_DIR}"
		echo "Release directory removed."
	fi
	exit 0
fi

# ---- release status check -----------------------------------------------
DEPLOYMENTID_FILE="${RELEASE_DIR%/}_DEPLOYMENTID.txt"

if [[ ! -f "${DEPLOYMENTID_FILE}" ]]; then
	echo "Error: Deployment ID file not found: ${DEPLOYMENTID_FILE}" >&2
	echo "Run sonatype-upload.sh first to create a deployment." >&2
	exit 1
fi

DEPLOYMENT_ID=$(cat "${DEPLOYMENTID_FILE}")

if [[ -z "${DEPLOYMENT_ID}" ]]; then
	echo "Error: Deployment ID file is empty: ${DEPLOYMENTID_FILE}" >&2
	exit 1
fi

echo "Checking release deployment status ..."
echo "  Deployment ID: ${DEPLOYMENT_ID}"
echo "  Status URL: ${STATUS_URL}"

STATUS_RESPONSE=$(curl -sS \
	-H "Authorization: Bearer ${SONATYPE_BEARER}" \
	"${STATUS_URL}?id=${DEPLOYMENT_ID}" 2>&1) || true

# Extract deploymentState from JSON response (portable, no grep -P)
STATE=$(echo "${STATUS_RESPONSE}" | sed -n 's/.*"deploymentState"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

echo "Deployment state: ${STATE:-UNKNOWN}"

case "${STATE}" in
	PUBLISHED)
		echo "Deployment successfully published."
		if [[ "${CLEAN}" == "true" ]]; then
			echo "Cleaning up release directory: ${RELEASE_DIR}"
			rm -rf "${RELEASE_DIR}"
			rm -f "${DEPLOYMENTID_FILE}"
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
