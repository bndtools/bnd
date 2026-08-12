#!/usr/bin/env bash
# test-jfrog-snapshot-token.sh — verify JFROG_SNAPSHOT_TOKEN has exactly the rights
# cibuild.yml "Publish P2 repo" needs: read + delete + write) on p2/pr/**.
#
# Usage:
#   JFROG_SNAPSHOT_TOKEN=<token> \
#     ./.github/workflows/test-jfrog-snapshot-token.sh
# Corporate TLS interception: CURL_EXTRA_OPTS=--ssl-no-revoke
set -euo pipefail
IFS=$'\n\t'

JFROG_URL="${JFROG_URL:-https://bndtools.jfrog.io}"

err() { printf '%s: %s\n' "${0##*/}" "$*" >&2; }
die() { err "$@"; exit 1; }
require_cmd() { command -v "$1" >/dev/null || die "required: $1"; }

require_cmd curl
[[ -n "${JFROG_SNAPSHOT_TOKEN:-}" ]] || die "JFROG_SNAPSHOT_TOKEN not set"

bearer=(-H "Authorization: Bearer ${JFROG_SNAPSHOT_TOKEN}")
PROBE_URL="${JFROG_URL}/artifactory/p2/pr/token-rights-probe-$$-${RANDOM}"

http() { # http METHOD URL [curl args...] -> echoes status code
	local method=$1 url=$2; shift 2
	local args=(--silent --show-error ${CURL_EXTRA_OPTS:-} --output /dev/null --write-out '%{http_code}')
	if [[ "$method" == "HEAD" ]]; then args+=(--head); else args+=(--request "$method"); fi
	curl "${args[@]}" "$@" "$url" || true
}

NAMES=(); STATES=()
check() { # check NAME status allowed...
	local name=$1 status=$2; shift 2
	local s ok="FAIL"
	for s in "$@"; do [[ "$status" == "$s" ]] && ok="PASS"; done
	NAMES+=("$name"); STATES+=("$ok (HTTP $status)")
	err "$name: $ok (HTTP $status)"
}

tmpfile=""
cleanup() {
	[[ -n "$tmpfile" ]] && rm -f "$tmpfile"
	curl --silent ${CURL_EXTRA_OPTS:-} --output /dev/null --request DELETE "${bearer[@]}" "$PROBE_URL" || true
}
trap cleanup EXIT

main() {
	local status probe
	tmpfile="$(mktemp)"
	printf 'token rights probe %s\n' "$(date -u +%FT%TZ)" > "$tmpfile"

	# token validity — 401 = expired/invalid, stop early
	status="$(http HEAD "${JFROG_URL}/artifactory/p2/pr/" "${bearer[@]}")"
	[[ "$status" == "401" ]] && die "token rejected (HTTP 401) — expired or invalid"

	# write p2/pr/** (granted by pt-p2-pr; also stages the read/delete tests)
	status="$(http PUT "${PROBE_URL}/probe.txt" "${bearer[@]}" --upload-file "$tmpfile")"
	check "write  p2/pr/**" "$status" 201

	# read p2/pr/** — workflow HEAD existence check [REQUIRED]
	status="$(http HEAD "${PROBE_URL}/probe.txt" "${bearer[@]}")"
	check "read   p2/pr/** [REQUIRED]" "$status" 200

	# delete p2/pr/** — workflow deletes previous deployment [REQUIRED]
	status="$(http DELETE "$PROBE_URL" "${bearer[@]}")"
	check "delete p2/pr/** [REQUIRED]" "$status" 200 204

	# negative scope probes — expect denial (403, or 404 when unauthorized paths are hidden)
	probe="scope-probe-$$-${RANDOM}.txt"
	status="$(http PUT "${JFROG_URL}/artifactory/libs-snapshot-local/${probe}" "${bearer[@]}" --upload-file "$tmpfile")"
	[[ "$status" == "201" ]] && http DELETE "${JFROG_URL}/artifactory/libs-snapshot-local/${probe}" "${bearer[@]}" >/dev/null
	check "no write libs-snapshot-local" "$status" 403 404

	status="$(http PUT "${JFROG_URL}/artifactory/libs-release-local/${probe}" "${bearer[@]}" --upload-file "$tmpfile")"
	[[ "$status" == "201" ]] && http DELETE "${JFROG_URL}/artifactory/libs-release-local/${probe}" "${bearer[@]}" >/dev/null
	check "no write libs-release-local" "$status" 403 404

	status="$(http DELETE "${JFROG_URL}/artifactory/p2/${probe%.txt}" "${bearer[@]}")"
	check "no delete p2 outside pr/**" "$status" 403 404

	err ""
	err "summary (${JFROG_URL}):"
	local i fail=0
	for i in "${!NAMES[@]}"; do
		err "  ${STATES[$i]%% *}  ${NAMES[$i]}  ${STATES[$i]#* }"
		[[ "${STATES[$i]}" == FAIL* ]] && fail=$((fail + 1))
	done
	((fail == 0)) || die "$fail check(s) failed"
	err "all checks passed"
}

main "$@"
