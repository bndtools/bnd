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
#   --status-url <url>                      Status API URL (release only)
#   --deployment-id <id>                    Use deployment ID directly (skip reading from file)
#   --diff                                  Compare deployed JARs with previous release in Maven Central
#   --verbose                               Show progress details during checks
#   --clean                                 Remove release-dir after successful check
#
# Environment:
#   SONATYPE_BEARER   – Bearer token for authentication (required)
#
# -------------------------------------------------------------------------
set -euo pipefail

# ---- defaults -----------------------------------------------------------
STATUS_URL="https://central.sonatype.com/api/v1/publisher/status"
DEPLOYMENT_FILES_URL="https://central.sonatype.com/api/v1/publisher/deployments/files"
CLEAN=false
DIFF=false
VERBOSE=false
RELEASE_DIR=""
DEPLOYMENT_ID=""
# -------------------------------------------------------------------------

usage() {
	cat <<-EOF
	Usage: $(basename "$0") [options] [<release-dir>]

	Check Sonatype deployment status and optionally clean up.

	Options:
	  --status-url <url>                Status API URL (default: Sonatype Central Portal)
	  --deployment-id <id>              Deployment ID to check (skip reading from file)
	  --diff                            Check deployed JAR artifact existence in previous Maven Central release
	  --verbose                         Show progress details
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
		--status-url)   STATUS_URL="$2"; shift 2 ;;
		--deployment-id) DEPLOYMENT_ID="$2"; shift 2 ;;
		--diff)         DIFF=true; shift ;;
		--verbose)      VERBOSE=true; shift ;;
		--clean)        CLEAN=true; shift ;;
		-h|--help)      usage 0 ;;
		-*)             echo "Unknown option: $1" >&2; usage 1 ;;
		*)              RELEASE_DIR="$1"; shift ;;
	esac
done

get_deployment_jar_paths() {
	local deployment_id="$1"
	local response http_status body payload

	# Schema requires sortField; deploymentIds narrows the result set.
	payload=$(printf '{"page":0,"size":500,"sortField":"createdTimestamp","sortDirection":"desc","deploymentIds":["%s"]}' "${deployment_id}")
	response=$(curl -sS \
		--request POST \
		-H "Authorization: Bearer ${AUTH_BEARER_TOKEN}" \
		-H "Accept: application/json" \
		-H "Content-Type: application/json" \
		-d "${payload}" \
		-w $'\nHTTP_STATUS:%{http_code}' \
		"${DEPLOYMENT_FILES_URL}" 2>&1) || true
	http_status=$(echo "${response}" | sed -n 's/^HTTP_STATUS:\([0-9][0-9][0-9]\)$/\1/p')
	body=$(echo "${response}" | sed '/^HTTP_STATUS:[0-9][0-9][0-9]$/d')

	if [[ -z "${http_status}" || "${http_status}" -ge 400 ]]; then
		echo "Error: Unable to browse deployment files (HTTP ${http_status:-UNKNOWN})." >&2
		echo "${body}" >&2
		return 1
	fi

	# Extract deployment file path fields that end with .jar.
	# API responses may use either "relativePath" or "path".
	echo "${body}" \
		| tr ',' '\n' \
		| sed -n 's/.*"relativePath"[[:space:]]*:[[:space:]]*"\([^"\\]*\.jar\)".*/\1/p; s/.*"relativePath"[[:space:]]*:[[:space:]]*"\([^"\\]*\\\/[^"\\]*\.jar\)".*/\1/p; s/.*"path"[[:space:]]*:[[:space:]]*"\([^"\\]*\.jar\)".*/\1/p; s/.*"path"[[:space:]]*:[[:space:]]*"\([^"\\]*\\\/[^"\\]*\.jar\)".*/\1/p' \
		| sed 's#\\/#/#g' \
		| sed 's#^/*##' \
		| awk '/\// { print }' \
		| sort -u
}

compare_with_previous_central_jars() {
	local deployment_id="$1"
	local central_base="https://repo1.maven.org/maven2"
	local rel_path rel_dir version artifact_id group_path file_name base_name prefix classifier jar_paths
	local metadata_url metadata versions previous_version remote_name remote_url
	local total checked exists_count missing_count skipped missing_previous missing_remote parse_errors total_jars

	total=0
	checked=0
	exists_count=0
	missing_count=0
	skipped=0
	missing_previous=0
	missing_remote=0
	parse_errors=0
	total_jars=0

	echo "Running --diff artifact existence check against Maven Central (using deployment files) ..."
	if [[ "${VERBOSE}" == "true" ]]; then
		echo "Fetching deployment file list ..."
	fi

	jar_paths=$(get_deployment_jar_paths "${deployment_id}") || return 1

	if [[ -z "${jar_paths}" ]]; then
		echo "Error: No JAR files found in deployment ${deployment_id}." >&2
		return 1
	fi

	total_jars=$(printf '%s\n' "${jar_paths}" | grep -c . || true)
	if [[ "${VERBOSE}" == "true" ]]; then
		echo "Found ${total_jars} deployment JARs to check."
	fi

	while IFS= read -r rel_path; do
		[[ -z "${rel_path}" ]] && continue
		total=$((total + 1))
		if [[ "${VERBOSE}" == "true" ]] && (( total == 1 || total % 25 == 0 )); then
			echo "Progress: ${total}/${total_jars}"
		fi
		rel_dir="$(dirname "${rel_path}")"
		version="$(basename "${rel_dir}")"
		artifact_id="$(basename "$(dirname "${rel_dir}")")"
		group_path="$(dirname "$(dirname "${rel_dir}")")"
		file_name="$(basename "${rel_path}")"
		base_name="${file_name%.jar}"
		prefix="${artifact_id}-${version}"

		if [[ "${base_name}" == "${prefix}" ]]; then
			classifier=""
		elif [[ "${base_name}" == "${prefix}-"* ]]; then
			classifier="${base_name#"${prefix}-"}"
		else
			echo "WARN: Skipping unrecognized jar filename pattern: ${rel_path}"
			parse_errors=$((parse_errors + 1))
			skipped=$((skipped + 1))
			continue
		fi

		metadata_url="${central_base}/${group_path}/${artifact_id}/maven-metadata.xml"
		if ! metadata=$(curl --connect-timeout 10 --max-time 30 -fsSL "${metadata_url}" 2>/dev/null); then
			echo "WARN: Could not read metadata from Maven Central: ${metadata_url}"
			skipped=$((skipped + 1))
			continue
		fi

		versions=$(printf '%s\n' "${metadata}" | sed -n 's|.*<version>\([^<]*\)</version>.*|\1|p')
		previous_version=$(printf '%s\n' "${versions}" \
			| grep -v '^[[:space:]]*$' \
			| grep -v 'SNAPSHOT' \
			| grep -Fxv "${version}" \
			| sort -V \
			| tail -n 1)

		if [[ -z "${previous_version}" ]]; then
			echo "INFO: No previous released version found for ${group_path//\//.}:${artifact_id}:${version}"
			missing_previous=$((missing_previous + 1))
			skipped=$((skipped + 1))
			continue
		fi

		remote_name="${artifact_id}-${previous_version}"
		if [[ -n "${classifier}" ]]; then
			remote_name="${remote_name}-${classifier}"
		fi
		remote_name="${remote_name}.jar"
		remote_url="${central_base}/${group_path}/${artifact_id}/${previous_version}/${remote_name}"

		checked=$((checked + 1))
		if curl --connect-timeout 10 --max-time 30 -fsSI "${remote_url}" >/dev/null 2>&1 \
			|| curl --connect-timeout 10 --max-time 30 -fsSL "${remote_url}" -o /dev/null 2>/dev/null; then
			exists_count=$((exists_count + 1))
			if [[ "${VERBOSE}" == "true" ]]; then
				echo "EXISTS: ${rel_path} -> ${artifact_id}-${previous_version}${classifier:+-${classifier}}.jar"
			fi
		else
			missing_count=$((missing_count + 1))
			missing_remote=$((missing_remote + 1))
			echo "MISSING: ${rel_path} -> ${artifact_id}-${previous_version}${classifier:+-${classifier}}.jar"
		fi
	done <<< "${jar_paths}"

	echo
	echo "--diff summary"
	echo "  Total deployment jars: ${total}"
	echo "  Checked existence: ${checked}"
	echo "  Exists: ${exists_count}"
	echo "  Missing: ${missing_count}"
	echo "  Skipped: ${skipped}"
	echo "    No previous version: ${missing_previous}"
	echo "    Missing in previous version: ${missing_remote}"
	echo "    Unrecognized filename pattern: ${parse_errors}"
}

# SONATYPE_BEARER is required for status checks.
AUTH_BEARER_TOKEN="${SONATYPE_BEARER:-}"

if [[ -z "${AUTH_BEARER_TOKEN}" ]]; then
	echo "Error: SONATYPE_BEARER is not configured." >&2
	exit 1
fi

if [[ -n "${RELEASE_DIR}" && -n "${DEPLOYMENT_ID}" ]]; then
	echo "Error: provide either --deployment-id or <release-dir>, not both" >&2
	usage 1
fi

if [[ -z "${RELEASE_DIR}" && -z "${DEPLOYMENT_ID}" ]]; then
	echo "Error: either --deployment-id or release directory argument is required" >&2
	usage 1
fi

# ---- release status check -----------------------------------------------
DEPLOYMENTID_FILE=""

if [[ -z "${DEPLOYMENT_ID}" ]]; then
	DEPLOYMENTID_FILE="${RELEASE_DIR%/}_DEPLOYMENTID.txt"

	if [[ ! -f "${DEPLOYMENTID_FILE}" ]]; then
		echo "Error: Deployment ID file not found: ${DEPLOYMENTID_FILE}" >&2
		echo "Run sonatype-upload.sh first to create a deployment." >&2
		exit 1
	fi

	DEPLOYMENT_ID=$(cat "${DEPLOYMENTID_FILE}")
fi

if [[ -z "${DEPLOYMENT_ID}" ]]; then
	echo "Error: Deployment ID file is empty: ${DEPLOYMENTID_FILE}" >&2
	exit 1
fi

echo "Checking release deployment status ..."
echo "  Deployment ID: ${DEPLOYMENT_ID}"
echo "  Status URL: ${STATUS_URL}"

STATUS_RESPONSE=$(curl -sS \
	--request POST \
	-H "Authorization: Bearer ${AUTH_BEARER_TOKEN}" \
	-H "Accept: application/json" \
	-w $'\nHTTP_STATUS:%{http_code}' \
	"${STATUS_URL}?id=${DEPLOYMENT_ID}" 2>&1) || true

HTTP_STATUS=$(echo "${STATUS_RESPONSE}" | sed -n 's/^HTTP_STATUS:\([0-9][0-9][0-9]\)$/\1/p')
STATUS_BODY=$(echo "${STATUS_RESPONSE}" | sed '/^HTTP_STATUS:[0-9][0-9][0-9]$/d')

if [[ -z "${HTTP_STATUS}" ]]; then
	echo "Error: Failed to determine HTTP status from Sonatype response." >&2
	echo "${STATUS_RESPONSE}" >&2
	exit 1
fi

if [[ "${HTTP_STATUS}" -ge 400 ]]; then
	echo "Error: Sonatype status API returned HTTP ${HTTP_STATUS}." >&2
	echo "${STATUS_BODY}" >&2
	exit 1
fi

# Extract deploymentState from JSON response (portable, no grep -P)
STATE=$(echo "${STATUS_BODY}" | sed -n 's/.*"deploymentState"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

echo "Deployment state: ${STATE:-UNKNOWN}"

case "${STATE}" in
	PUBLISHED)
		echo "Deployment successfully published."
		if [[ "${DIFF}" == "true" ]]; then
			compare_with_previous_central_jars "${DEPLOYMENT_ID}"
		fi
		if [[ "${CLEAN}" == "true" && -n "${RELEASE_DIR}" ]]; then
			echo "Cleaning up release directory: ${RELEASE_DIR}"
			rm -rf "${RELEASE_DIR}"
			rm -f "${DEPLOYMENTID_FILE}"
			echo "Release directory and deployment ID file removed."
		elif [[ "${CLEAN}" == "true" ]]; then
			echo "--clean ignored: no release directory was provided."
		fi
		exit 0
		;;
	VALIDATED)
		echo "Deployment validated (awaiting manual publishing)."
		if [[ "${DIFF}" == "true" ]]; then
			compare_with_previous_central_jars "${DEPLOYMENT_ID}"
		fi
		exit 0
		;;
	PENDING|VALIDATING|PUBLISHING)
		echo "Deployment is still in progress (${STATE})."
		exit 2
		;;
	FAILED)
		echo "Error: Deployment failed." >&2
		echo "${STATUS_BODY}" >&2
		exit 1
		;;
	*)
		echo "Unknown deployment state: ${STATE:-UNKNOWN}" >&2
		echo "${STATUS_BODY}" >&2
		exit 1
		;;
esac
