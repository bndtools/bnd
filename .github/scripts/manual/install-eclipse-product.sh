#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# --- Script internal settings ---
EQUINOX_DROP_URL="${EQUINOX_DROP_URL:-https://download.eclipse.org/equinox/drops/R-4.41-202608281142}"
STARTER_KIT_VERSION="${STARTER_KIT_VERSION:-4.41}"
JAVA_MIN_VERSION="${JAVA_MIN_VERSION:-21}"
PULLREQUESTS="${PULLREQUESTS:-7430}"
SIMREL_REPOSITORY="${SIMREL_REPOSITORY:-https://download.eclipse.org/releases/2026-09/202609091000}"
ECLIPSE_REPOSITORY="${ECLIPSE_REPOSITORY:-https://download.eclipse.org/eclipse/updates/4.41/}"
PRODUCT_IU="${PRODUCT_IU:-org.eclipse.platform.ide}"
BNDTOOLS_IUS="${BNDTOOLS_IUS:-bndtools.pde.feature.feature.group,bndtools.m2e.feature.feature.group,bndtools.main.feature.feature.group}"
PROFILE="${PROFILE:-PlatformProfile}"
PROGRESS_INTERVAL="${PROGRESS_INTERVAL:-15}"

# --- Relative path settings (derived from RUNTIME_ROOT/RUN_DIR) ---
CACHE_DIR="${CACHE_DIR:-${XDG_CACHE_HOME:-$HOME/.cache}/eclipse-starterkit}"
RUNTIME_ROOT="${RUNTIME_ROOT:-$PWD/runtime}"
TIMESTAMP="${TIMESTAMP:-$(date +%Y%m%d-%H%M%S)}"
RUN_DIR="${RUN_DIR:-$RUNTIME_ROOT/$TIMESTAMP}"
DESTINATION="${DESTINATION:-$RUN_DIR/instance}"
DATA_DIR="${DATA_DIR:-$RUN_DIR/data}"
LOG_FILE="${LOG_FILE:-$RUN_DIR/p2-director.log}"

BNDTOOLS_REPOSITORY="${BNDTOOLS_REPOSITORY:-https://bndtools.jfrog.io/artifactory/p2/pr/$PULLREQUESTS/}"
REPOSITORIES="${REPOSITORIES:-$SIMREL_REPOSITORY,$ECLIPSE_REPOSITORY,$BNDTOOLS_REPOSITORY}"
INSTALL_IUS="${INSTALL_IUS:-$PRODUCT_IU,$BNDTOOLS_IUS}"

err() {
	printf '%s: %s\n' "${0##*/}" "$*" >&2
}

fetch_file() {
	local url="$1"
	local output="$2"
	if curl --help all 2>/dev/null | grep -q -- '--ssl-no-revoke'; then
		curl -sSL --ssl-no-revoke "$url" -o "$output" || curl -sSL -k "$url" -o "$output"
	else
		curl -sSL "$url" -o "$output" || curl -sSL -k "$url" -o "$output"
	fi
}

check_java_home() {
	if [[ -z "${JAVA_HOME:-}" ]]; then
		err "JAVA_HOME is not set; export JAVA_HOME pointing to a JDK $JAVA_MIN_VERSION+ installation"
		exit 1
	fi

	JAVA_COMMAND="$JAVA_HOME/bin/java"
	if [[ ! -x "$JAVA_COMMAND" ]]; then
		JAVA_COMMAND="$JAVA_HOME/bin/java.exe"
	fi
	if [[ ! -x "$JAVA_COMMAND" ]]; then
		err "Java command not executable under JAVA_HOME: $JAVA_HOME"
		exit 1
	fi

	local version_output major_version
	version_output="$("$JAVA_COMMAND" -version 2>&1)"
	major_version="$(printf '%s' "$version_output" | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n1)"
	if [[ -z "$major_version" ]]; then
		err "Unable to determine Java version from: $JAVA_COMMAND"
		exit 1
	fi
	if ((major_version < JAVA_MIN_VERSION)); then
		err "JAVA_HOME points to Java $major_version; Java $JAVA_MIN_VERSION+ is required: $JAVA_HOME"
		exit 1
	fi
}

detect_target() {
	local uname_s uname_m
	uname_s="$(uname -s)"
	uname_m="$(uname -m)"

	case "$uname_s" in
		CYGWIN*|MINGW*|MSYS*)
			DETECTED_OS="win32"
			DETECTED_WS="win32"
			;;
		Darwin*)
			DETECTED_OS="macosx"
			DETECTED_WS="cocoa"
			;;
		Linux*)
			DETECTED_OS="linux"
			DETECTED_WS="gtk"
			;;
		*)
			err "unsupported operating system: $uname_s"
			exit 1
			;;
	esac

	case "$uname_m" in
		x86_64|amd64)
			DETECTED_ARCH="x86_64"
			;;
		aarch64|arm64)
			DETECTED_ARCH="aarch64"
			;;
		ppc64le)
			DETECTED_ARCH="ppc64le"
			;;
		riscv64)
			DETECTED_ARCH="riscv64"
			;;
		*)
			err "unsupported CPU architecture: $uname_m"
			exit 1
			;;
	esac
}

detect_target
TARGET_OS="${TARGET_OS:-$DETECTED_OS}"
TARGET_WS="${TARGET_WS:-$DETECTED_WS}"
TARGET_ARCH="${TARGET_ARCH:-$DETECTED_ARCH}"

if [[ -z "${STARTER_KIT_ARCHIVE:-}" ]]; then
	if [[ "$TARGET_OS" == "win32" ]]; then
		STARTER_KIT_ARCHIVE="EclipseRT-OSGi-StarterKit-${STARTER_KIT_VERSION}-${TARGET_OS}-${TARGET_WS}-${TARGET_ARCH}.zip"
	else
		STARTER_KIT_ARCHIVE="EclipseRT-OSGi-StarterKit-${STARTER_KIT_VERSION}-${TARGET_OS}-${TARGET_WS}-${TARGET_ARCH}.tar.gz"
	fi
fi

STARTER_KIT_URL="${STARTER_KIT_URL:-$EQUINOX_DROP_URL/$STARTER_KIT_ARCHIVE}"
EXTRACT_NAME="${STARTER_KIT_ARCHIVE%.tar.gz}"
EXTRACT_NAME="${EXTRACT_NAME%.zip}"
EXTRACT_DIR="$CACHE_DIR/extracted/$EXTRACT_NAME"
CACHED_ARCHIVE="$CACHE_DIR/$STARTER_KIT_ARCHIVE"

cleanup() {
	if [[ -n "${progress_pid:-}" ]]; then
		kill "$progress_pid" 2>/dev/null || true
		wait "$progress_pid" 2>/dev/null || true
	fi
	if [[ -n "${director_pid:-}" ]]; then
		kill "$director_pid" 2>/dev/null || true
	fi
	rm -rf "${temp_config:-}" "${trace_options:-}"
}

progress_reporter() {
	local pid="$1"
	local started="$SECONDS"
	local log_size
	while kill -0 "$pid" 2>/dev/null; do
		sleep "$PROGRESS_INTERVAL"
		if kill -0 "$pid" 2>/dev/null; then
			log_size="$(wc -c < "$LOG_FILE" 2>/dev/null || echo 0)"
			printf '[%s] p2 director running: %ss elapsed, %s log bytes\n' \
				"$(date '+%Y-%m-%d %H:%M:%S')" "$((SECONDS - started))" "$log_size"
		fi
	done
}

trap cleanup EXIT INT TERM

if [[ ! "$PROGRESS_INTERVAL" =~ ^[1-9][0-9]*$ ]]; then
	err "PROGRESS_INTERVAL must be a positive integer: $PROGRESS_INTERVAL"
	exit 2
fi

mkdir -p "$CACHE_DIR" "$RUN_DIR" "$DESTINATION" "$DATA_DIR" "$(dirname "$LOG_FILE")"
touch "$LOG_FILE"
exec > >(tee -a "$LOG_FILE") 2>&1

check_java_home

if [[ ! -f "$CACHED_ARCHIVE" ]]; then
	printf 'Downloading starter kit: %s -> %s\n' "$STARTER_KIT_URL" "$CACHED_ARCHIVE"
	fetch_file "$STARTER_KIT_URL" "$CACHED_ARCHIVE"
fi

if [[ ! -d "$EXTRACT_DIR/rt" ]]; then
	printf 'Extracting starter kit into: %s\n' "$EXTRACT_DIR"
	mkdir -p "$EXTRACT_DIR"
	if [[ "$CACHED_ARCHIVE" == *.zip ]]; then
		unzip -q -o "$CACHED_ARCHIVE" -d "$EXTRACT_DIR"
	else
		tar -xzf "$CACHED_ARCHIVE" -C "$EXTRACT_DIR"
	fi
fi

STARTER_KIT_HOME="$EXTRACT_DIR/rt"
launcher_jar=""
while IFS= read -r -d '' candidate; do
	launcher_jar="$candidate"
	break
done < <(find "$STARTER_KIT_HOME/plugins" -maxdepth 1 -type f \
	-name 'org.eclipse.equinox.launcher_*.jar' -print0)

if [[ -z "$launcher_jar" ]]; then
	err "Equinox launcher JAR not found in: $STARTER_KIT_HOME/plugins"
	exit 1
fi

for companion_bundle in \
	org.eclipse.equinox.p2.director.app_1.3.1000.v20260305-0817.jar \
	org.eclipse.equinox.p2.publisher_1.9.700.v20260512-1529.jar \
	org.eclipse.equinox.p2.publisher.eclipse_1.6.900.v20260809-0656.jar \
	org.eclipse.equinox.p2.repository.tools_2.4.1000.v20260304-1232.jar
do
	bundle_target="$STARTER_KIT_HOME/plugins/$companion_bundle"
	if [[ ! -f "$bundle_target" ]]; then
		printf 'Fetching director dependency: %s\n' "$companion_bundle"
		fetch_file "$EQUINOX_DROP_URL/$companion_bundle" "$bundle_target"
	fi
done

temp_config="$(mktemp -d "$RUN_DIR/director-conf-XXXXXX")"
cp -r "$STARTER_KIT_HOME/configuration/"* "$temp_config/"
sed -i 's/eclipse.ignoreApp=true/eclipse.ignoreApp=false/' "$temp_config/config.ini"
sed -i 's/osgi.noShutdown=true/osgi.noShutdown=false/' "$temp_config/config.ini"

for companion_bundle in \
	org.eclipse.equinox.p2.director.app_1.3.1000.v20260305-0817.jar \
	org.eclipse.equinox.p2.publisher_1.9.700.v20260512-1529.jar \
	org.eclipse.equinox.p2.publisher.eclipse_1.6.900.v20260809-0656.jar \
	org.eclipse.equinox.p2.repository.tools_2.4.1000.v20260304-1232.jar
do
	grep -q "$companion_bundle" "$temp_config/config.ini" || \
		sed -i "s/osgi.bundles=/osgi.bundles=reference\\\\:file\\\\:$companion_bundle@4,/" "$temp_config/config.ini"
done

sed -i 's/org.eclipse.core.runtime_[^,]*@4/&\\:start/g' "$temp_config/config.ini"
sed -i 's/org.apache.felix.scr_[^,]*@4/&\\:start/g' "$temp_config/config.ini"
sed -i 's/org.eclipse.equinox.app_[^,]*@4/&\\:start/g' "$temp_config/config.ini"
sed -i 's/org.eclipse.equinox.registry_[^,]*@4/&\\:start/g' "$temp_config/config.ini"
sed -i 's/org.eclipse.equinox.p2.director.app_[^,]*@[^,]*/org.eclipse.equinox.p2.director.app_1.3.1000.v20260305-0817.jar@start/g' "$temp_config/config.ini"

trace_options="$(mktemp "$RUN_DIR/trace-options-XXXXXX")"
cat > "$trace_options" <<'EOF'
org.eclipse.equinox.p2.core/debug=true
org.eclipse.equinox.p2.core/artifacts/mirrors=true
org.eclipse.equinox.p2.core/metadata/parsing=true
org.eclipse.equinox.p2.core/parseproblems=true
org.eclipse.equinox.p2.engine/engine/debug=true
org.eclipse.equinox.p2.engine/enginesession/debug=true
org.eclipse.equinox.p2.engine/profileregistry/debug=true
org.eclipse.equinox.p2.repository/transport/debug=true
org.eclipse.equinox.p2.repository/credentials/debug=true
EOF

printf '\n=== p2 Director Installation ===\n'
printf 'Starter Kit:  %s\n' "$STARTER_KIT_HOME"
printf 'Target:       os=%s ws=%s arch=%s\n' "$TARGET_OS" "$TARGET_WS" "$TARGET_ARCH"
printf 'Repositories: %s\n' "$REPOSITORIES"
printf 'Installing:   %s\n' "$INSTALL_IUS"
printf 'Destination:  %s\n' "$DESTINATION"
printf 'Data Area:    %s\n' "$DATA_DIR"
printf 'Log:          %s\n\n' "$LOG_FILE"

"$JAVA_COMMAND" -jar "$launcher_jar" \
	-configuration "$temp_config" \
	-nosplash \
	-debug "$trace_options" \
	-application org.eclipse.equinox.p2.director \
	-repository "$REPOSITORIES" \
	-installIU "$INSTALL_IUS" \
	-destination "$DESTINATION" \
	-bundlepool "$DESTINATION" \
	-profile "$PROFILE" \
	-profileProperties org.eclipse.update.install.features=true \
	-p2.os "$TARGET_OS" \
	-p2.ws "$TARGET_WS" \
	-p2.arch "$TARGET_ARCH" \
	-roaming \
	-consoleLog &
director_pid="$!"
progress_reporter "$director_pid" &
progress_pid="$!"

set +e
wait "$director_pid"
director_status="$?"
set -e
director_pid=""
kill "$progress_pid" 2>/dev/null || true
wait "$progress_pid" 2>/dev/null || true
progress_pid=""

if ((director_status != 0)); then
	err "p2 director failed with exit code $director_status; see $LOG_FILE"
	exit "$director_status"
fi

printf '\n=== Validating Installed Eclipse Product ===\n'
installed_launcher_jar=""
while IFS= read -r -d '' candidate; do
	installed_launcher_jar="$candidate"
	break
done < <(find "$DESTINATION/plugins" -maxdepth 1 -type f \
	-name 'org.eclipse.equinox.launcher_*.jar' -print0)

if [[ -z "$installed_launcher_jar" ]]; then
	err "Installed Equinox launcher JAR not found under: $DESTINATION/plugins"
	exit 1
fi

installed_exe=""
for candidate in "$DESTINATION/eclipse.exe" "$DESTINATION/eclipsec.exe" "$DESTINATION/eclipse"; do
	if [[ -x "$candidate" ]]; then
		installed_exe="$candidate"
		break
	fi
done

printf 'Installed launcher jar: %s\n' "$installed_launcher_jar"
if [[ -n "$installed_exe" ]]; then
	printf 'Installed executable:   %s\n' "$installed_exe"
fi

printf 'Running headless application verification...\n'
set +e
"$JAVA_COMMAND" -jar "$installed_launcher_jar" \
	-configuration "$DESTINATION/configuration" \
	-nosplash \
	-data "$DATA_DIR" \
	-application org.eclipse.ant.core.antRunner \
	-version \
	-consoleLog
validation_status="$?"
set -e

if ((validation_status != 0)); then
	err "Installed product launch failed with exit code $validation_status"
	exit "$validation_status"
fi

printf '\n=== Log Verification ===\n'
workspace_log="$DATA_DIR/.metadata/.log"
if [[ -f "$workspace_log" ]]; then
	printf 'Found workspace log: %s\n' "$workspace_log"
	if grep -E '!MESSAGE|!ENTRY|!SUBENTRY' "$workspace_log" >/dev/null 2>&1; then
		printf 'Recent log entries:\n'
		tail -n 20 "$workspace_log"
	fi
else
	printf 'No errors logged in workspace log (%s)\n' "$workspace_log"
fi

printf '\np2 installation and product launch validation completed successfully.\n'
