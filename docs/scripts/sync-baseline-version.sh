#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
DOCS_DIR="$ROOT_DIR/docs"
BUILD_BND="$ROOT_DIR/cnf/build.bnd"
DATA_FILE="$DOCS_DIR/_data/bnd_version.yml"

if [[ ! -f "$BUILD_BND" ]]; then
	echo "Cannot find $BUILD_BND" >&2
	exit 1
fi

baseline_version=$(awk -F':' '/^baseline\.version[[:space:]]*:/ {
	gsub(/[[:space:]]/, "", $2)
	print $2
	exit
}' "$BUILD_BND")

if [[ -z "$baseline_version" ]]; then
	echo "Could not extract baseline.version from $BUILD_BND" >&2
	exit 1
fi

cat > "$DATA_FILE" <<EOF
baseline_version: "$baseline_version"
EOF

echo "Synced docs baseline_version=$baseline_version"
