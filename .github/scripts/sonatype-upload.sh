#!/usr/bin/env bash
# -------------------------------------------------------------------------
# sonatype-upload.sh – Upload a local Maven repository folder to
# Sonatype Central Portal (https://central.sonatype.com).
#
# This script is build-tool independent and can be called after any
# combination of Gradle / Maven / bnd builds have populated a shared
# release directory.
#
# Supports only release deployments:
#   - Release: uploads via /api/v1/publisher/upload
#
# Note: uses `jar cMf` (from JDK) instead of `zip` for git bash compatibility.
#
# Usage:
#   SONATYPE_BEARER=<token> ./.github/scripts/sonatype-upload.sh [options] <release-dir>
#
# Options:
#   --publishing-type <AUTOMATIC|USER_MANAGED>   (default: USER_MANAGED, release only)
#   --name <deployment-name>                     (default: auto-generated)
#   --upload-url <url>                           (default: Sonatype Central Portal release URL)
#
# Environment:
#   SONATYPE_BEARER   – Bearer token for authentication (required)
#   INSECURE          – Set to 'true' to pass --insecure to curl (e.g. for testing behind a proxy)
#
# -------------------------------------------------------------------------
set -euo pipefail

# ---- defaults -----------------------------------------------------------
UPLOAD_URL="https://central.sonatype.com/api/v1/publisher/upload"
PUBLISHING_TYPE="USER_MANAGED"   # USER_MANAGED (manual) or AUTOMATIC
DEPLOYMENT_NAME=""
RELEASE_DIR=""
# -------------------------------------------------------------------------

# Detect distinct namespace roots (first two path segments of POM files,
# e.g. biz/aQute or org/bndtools) as required by Sonatype Central Portal.
detect_namespaces() {
	find "${RELEASE_DIR}" -type f -name '*.pom' -print0 | while IFS= read -r -d '' file; do
		rel_path="${file#"${RELEASE_DIR}"/}"
		printf '%s\n' "${rel_path}" | cut -d'/' -f1-2
	done | sort -u
}

# Upload one bundle, optionally scoped to a namespace subdirectory.
# Arguments: <ns_dir> <deployment_name>
#   ns_dir: path relative to RELEASE_DIR to include (empty = whole dir)
upload_namespace() {
	local ns_dir="$1"
	local name="$2"
	local ns_tag="${ns_dir:+-${ns_dir//\//-}}"
	local bundle_zip="${TMPDIR:-/tmp}/sonatype-bundle-$$${ns_tag}.zip"
	local http_response="${TMPDIR:-/tmp}/sonatype-response-$$${ns_tag}.txt"

	rm -f "${bundle_zip}" "${http_response}"
	TEMP_FILES+=("${bundle_zip}" "${http_response}")

	if [[ -z "${ns_dir}" ]]; then
		echo "Creating Sonatype Central bundle from ${RELEASE_DIR} ..."
		(cd "${RELEASE_DIR}" && jar cMf "${bundle_zip}" .)
	else
		echo "Creating Sonatype Central bundle for namespace ${ns_dir} ..."
		local filelist
		filelist=$(mktemp)
		TEMP_FILES+=("${filelist}")
		(cd "${RELEASE_DIR}" && find . -path "./${ns_dir}/*" -type f -print > "${filelist}" && jar cMf "${bundle_zip}" @"${filelist}")
		rm -f "${filelist}"
	fi

	local bundle_size
	bundle_size=$(stat -c%s "${bundle_zip}" 2>/dev/null || stat -f%z "${bundle_zip}")
	echo "Bundle size: ${bundle_size} bytes"

	if [[ "${bundle_size}" -eq 0 ]]; then
		echo "Error: bundle zip is empty – nothing to upload" >&2
		exit 1
	fi

	local encoded_name
	encoded_name=$(printf '%s' "${name}" | sed -e 's/%/%25/g' -e 's/ /%20/g' -e 's/&/%26/g' -e 's/=/%3D/g' -e 's/?/%3F/g' -e 's/+/%2B/g' -e 's/#/%23/g' -e 's/\[/%5B/g' -e 's/\]/%5D/g')
	local query="name=${encoded_name}&publishingType=${PUBLISHING_TYPE}"

	echo "Uploading bundle to Sonatype Central Portal ..."
	echo "  URL: ${UPLOAD_URL}"
	echo "  Publishing type: ${PUBLISHING_TYPE}"
	echo "  Name: ${name}"

	local curl_opts=()
	[[ "${INSECURE:-}" == "true" ]] && curl_opts+=("--insecure")

	local http_code
	http_code=$(curl -sS -w '%{http_code}' -o "${http_response}" \
		"${curl_opts[@]}" \
		-H "Authorization: Bearer ${SONATYPE_BEARER}" \
		-F "bundle=@${bundle_zip}" \
		"${UPLOAD_URL}?${query}")

	local response_body
	response_body=$(cat "${http_response}")

	if [[ "${http_code}" -lt 200 || "${http_code}" -ge 300 ]]; then
		echo "Error: Upload failed with HTTP ${http_code}" >&2
		echo "${response_body}" >&2
		exit 1
	fi

	local deployment_id="${response_body}"
	echo "Upload accepted. Deployment ID: ${deployment_id}"
	echo "${deployment_id}" >> "${DEPLOYMENTID_FILE}"
	rm -f "${bundle_zip}" "${http_response}"
}

usage() {
	cat <<-EOF
	Usage: $(basename "$0") [options] <release-dir>

	Upload a local Maven repository folder to Sonatype Central Portal.

	Options:
	  --publishing-type <AUTOMATIC|USER_MANAGED>  Publishing type (default: USER_MANAGED)
	  --name <name>                               Deployment name (default: auto-generated)
	  --upload-url <url>                           Release upload endpoint URL
	  -h, --help                                  Show this help message

	Environment:
	  SONATYPE_BEARER   Bearer token for authentication (required)
	  INSECURE          Set to 'true' to pass --insecure to curl
	EOF
	exit "${1:-0}"
}


# ---- parse arguments (robust: first non-option is dir, ignore trailing options) ----
RELEASE_DIR=""
while [[ $# -gt 0 ]]; do
	case "$1" in
		--publishing-type) PUBLISHING_TYPE="$2"; shift 2 ;;
		--name)            DEPLOYMENT_NAME="$2"; shift 2 ;;
		--upload-url)      UPLOAD_URL="$2"; shift 2 ;;
		-h|--help)         usage 0 ;;
		-*)                echo "Unknown option: $1" >&2; usage 1 ;;
		*)
			if [[ -z "$RELEASE_DIR" ]]; then
				RELEASE_DIR="$1"
			else
				# Ignore extra non-option args after dir
				:
			fi
			shift
			;;
	esac
done

if [[ -z "${RELEASE_DIR}" ]]; then
	echo "Error: release directory argument is required" >&2
	usage 1
fi

if [[ ! -d "${RELEASE_DIR}" ]]; then
	echo "Error: release directory does not exist: ${RELEASE_DIR}" >&2
	exit 1
fi

: "${SONATYPE_BEARER:?Error: SONATYPE_BEARER environment variable is not set}"

# Deployment ID file stored beside the release dir
DEPLOYMENTID_FILE="${RELEASE_DIR%/}_DEPLOYMENTID.txt"

# ---- detect namespaces and build deployment name ------------------------
TEMP_FILES=()
trap 'rm -f "${TEMP_FILES[@]}"' EXIT

mapfile -t NAMESPACES < <(detect_namespaces)

if [[ -z "${DEPLOYMENT_NAME}" ]]; then
	NS_LABEL=$(printf '%s\n' "${NAMESPACES[@]:-unknown}" | tr '/' '.' | paste -sd, -)
	DEPLOYMENT_NAME="uploaded ${NS_LABEL} on $(date '+%Y%m%d-%H%M%S')"
fi

# ---- upload: one bundle per namespace (Sonatype requires single namespace) ----
if [[ ${#NAMESPACES[@]} -le 1 ]]; then
	# Single namespace or no POM files found: upload everything as one bundle
	upload_namespace "" "${DEPLOYMENT_NAME}"
else
	echo "Multiple namespaces detected: ${NAMESPACES[*]} – uploading separately"
	for NS in "${NAMESPACES[@]}"; do
		NS_DOTTED="${NS//\//.}"
		upload_namespace "${NS}" "${DEPLOYMENT_NAME} [${NS_DOTTED}]"
	done
fi

echo "Deployment ID(s) stored in: ${DEPLOYMENTID_FILE}"
