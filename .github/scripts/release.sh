#!/usr/bin/env bash
#
# Release preparation script for bnd/bndtools
#
# This script automates the file modifications needed for releasing bnd.
# It handles three modes:
#   1. first-rc: Prepare master for V2 and next branch for V1.RC1
#   2. next-rc:  Update next branch from RCx to RC(x+1)
#   3. release:  Update next branch from RCx to final release
#
# JFROG updates are NOT handled by this script and must be done manually.
#
# Usage:
#   ./release.sh --mode first-rc --release-version 7.2.0 --next-version 7.3.0 --rc 1
#   ./release.sh --mode next-rc --release-version 7.2.0 --rc 2
#   ./release.sh --mode release --release-version 7.2.0
#
set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Detect if we're on macOS (BSD sed) or Linux (GNU sed)
if sed --version >/dev/null 2>&1; then
    # GNU sed (Linux)
    SED_INPLACE=(sed -i)
else
    # BSD sed (macOS)
    SED_INPLACE=(sed -i '')
fi

# Default values
MODE=""
RELEASE_VERSION=""
NEXT_VERSION=""
RC_NUMBER=""
DRY_RUN=false

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Automate bnd release preparation.

Options:
  -m, --mode MODE           Release mode: first-rc, prepare-next, next-rc, release, patch-first-rc, or patch-next-rc (required)
  -v, --release-version VER Version to release (V1), e.g., 7.2.0 or 7.2.1 (required)
  -n, --next-version VER    Next development version (V2), e.g., 7.3.0 (required for first-rc, prepare-next, and patch-first-rc)
  -r, --rc NUMBER           Release candidate number, e.g., 1, 2 (required for first-rc, prepare-next, next-rc, patch-first-rc, and patch-next-rc)
  --dry-run                 Show what would be changed without making changes
  -h, --help                Show this help message

Modes:
  first-rc      Prepare master branch for V2 (next development version)
                Requires: --release-version, --next-version, --rc
                Run this on the master branch first for a MINOR version release.

  prepare-next  Prepare next branch for first release candidate (V1.RC1)
                Requires: --release-version, --next-version, --rc
                Run this on the next branch after first-rc is done on master.

  next-rc       Update next branch from current RC to next RC
                Requires: --release-version, --rc (the NEW rc number)

  release       Update next branch from RC to final release
                Requires: --release-version

  patch-first-rc  Prepare next branch for first patch release candidate (V1.RC1)
                  Requires: --release-version, --next-version, --rc
                  Use for MICRO version releases (e.g., 7.2.1). Does NOT update master branch.
                  RC1 builds with the previous release version.

  patch-next-rc   Update next branch from current patch RC to next patch RC
                  Requires: --release-version, --next-version, --rc (the NEW rc number)
                  Use for subsequent patch RCs (RC2+). Builds with previous RC.

Examples:
  # Regular MINOR version release workflow:
  # Step 1: On master, prepare for 7.3.0 development stream
  $(basename "$0") --mode first-rc --release-version 7.2.0 --next-version 7.3.0 --rc 1

  # Step 2: On next, prepare for 7.2.0-RC1 release
  $(basename "$0") --mode prepare-next --release-version 7.2.0 --next-version 7.3.0 --rc 1

  # Subsequent RC: Update to 7.2.0-RC2
  $(basename "$0") --mode next-rc --release-version 7.2.0 --rc 2

  # Final release of 7.2.0
  $(basename "$0") --mode release --release-version 7.2.0

  # Patch MICRO version release workflow (e.g., 7.2.1 after 7.2.0 is released):
  # Step 1: On next (still on 7.2.0), prepare for 7.2.1-RC1 (cherry-pick fixes from master)
  $(basename "$0") --mode patch-first-rc --release-version 7.2.1 --next-version 7.3.0 --rc 1

  # Step 2: On next, update to 7.2.1-RC2
  $(basename "$0") --mode patch-next-rc --release-version 7.2.1 --next-version 7.3.0 --rc 2

  # Step 3: Final patch release of 7.2.1
  $(basename "$0") --mode release --release-version 7.2.1

Note: JFROG configuration updates must be done manually after running this script.
EOF
}

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--mode)
            MODE="$2"
            shift 2
            ;;
        -v|--release-version)
            RELEASE_VERSION="$2"
            shift 2
            ;;
        -n|--next-version)
            NEXT_VERSION="$2"
            shift 2
            ;;
        -r|--rc)
            RC_NUMBER="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Validate required arguments
validate_args() {
    if [[ -z "$MODE" ]]; then
        log_error "Mode is required"
        usage
        exit 1
    fi

    if [[ -z "$RELEASE_VERSION" ]]; then
        log_error "Release version is required"
        usage
        exit 1
    fi

    # Validate version format (major.minor.patch)
    if ! [[ "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        log_error "Invalid release version format. Expected: X.Y.Z (e.g., 7.2.0)"
        exit 1
    fi

    case "$MODE" in
        first-rc)
            if [[ -z "$NEXT_VERSION" ]]; then
                log_error "Next version is required for first-rc mode"
                usage
                exit 1
            fi
            if ! [[ "$NEXT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                log_error "Invalid next version format. Expected: X.Y.Z (e.g., 7.3.0)"
                exit 1
            fi
            if [[ -z "$RC_NUMBER" ]]; then
                log_error "RC number is required for first-rc mode"
                usage
                exit 1
            fi
            ;;
        next-rc)
            if [[ -z "$RC_NUMBER" ]]; then
                log_error "RC number is required for next-rc mode"
                usage
                exit 1
            fi
            ;;
        prepare-next)
            if [[ -z "$NEXT_VERSION" ]]; then
                log_error "Next version is required for prepare-next mode"
                usage
                exit 1
            fi
            if ! [[ "$NEXT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                log_error "Invalid next version format. Expected: X.Y.Z (e.g., 7.3.0)"
                exit 1
            fi
            if [[ -z "$RC_NUMBER" ]]; then
                log_error "RC number is required for prepare-next mode"
                usage
                exit 1
            fi
            ;;
        patch-first-rc)
            if [[ -z "$NEXT_VERSION" ]]; then
                log_error "Next version is required for patch-first-rc mode"
                usage
                exit 1
            fi
            if ! [[ "$NEXT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                log_error "Invalid next version format. Expected: X.Y.Z (e.g., 7.3.0)"
                exit 1
            fi
            if [[ -z "$RC_NUMBER" ]]; then
                log_error "RC number is required for patch-first-rc mode"
                usage
                exit 1
            fi
            # Validate that release version is a patch (MICRO version != 0)
            parse_version "$RELEASE_VERSION" "REL"
            if [[ "$REL_PATCH" == "0" ]]; then
                log_error "patch-first-rc mode requires a MICRO version > 0 (e.g., 7.2.1, not 7.2.0)"
                exit 1
            fi
            ;;
        patch-next-rc)
            if [[ -z "$NEXT_VERSION" ]]; then
                log_error "Next version is required for patch-next-rc mode"
                usage
                exit 1
            fi
            if ! [[ "$NEXT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                log_error "Invalid next version format. Expected: X.Y.Z (e.g., 7.3.0)"
                exit 1
            fi
            if [[ -z "$RC_NUMBER" ]]; then
                log_error "RC number is required for patch-next-rc mode"
                usage
                exit 1
            fi
            # Validate that release version is a patch (MICRO version != 0)
            parse_version "$RELEASE_VERSION" "REL"
            if [[ "$REL_PATCH" == "0" ]]; then
                log_error "patch-next-rc mode requires a MICRO version > 0 (e.g., 7.2.1, not 7.2.0)"
                exit 1
            fi
            ;;
        release)
            # No additional requirements
            ;;
        *)
            log_error "Invalid mode: $MODE. Must be: first-rc, prepare-next, next-rc, release, patch-first-rc, or patch-next-rc"
            usage
            exit 1
            ;;
    esac
}

# Extract version components into variables named ${prefix}_MAJOR, ${prefix}_MINOR, ${prefix}_PATCH
# Usage: parse_version "1.2.3" "VER" creates VER_MAJOR=1, VER_MINOR=2, VER_PATCH=3
parse_version() {
    local version=$1
    local var_prefix=$2
    IFS='.' read -r "${var_prefix}_MAJOR" "${var_prefix}_MINOR" "${var_prefix}_PATCH" <<< "$version"
}

# Update cnf/build.bnd
update_build_bnd() {
    local file="${REPO_ROOT}/cnf/build.bnd"
    local version=$1
    local snapshot_value=$2  # empty string for release, RCx for RC, commented out for snapshot

    log_info "Updating ${file}"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would set base.version to: $version"
        log_info "  Would set -snapshot to: $snapshot_value"
        return
    fi

    # Update base.version
    "${SED_INPLACE[@]}" "s/^base\.version:.*$/base.version:           $version/" "$file"

    if [[ -z "$snapshot_value" ]]; then
        # Final release: -snapshot: (empty value)
        if grep -q "^#-snapshot:" "$file"; then
            "${SED_INPLACE[@]}" "s/^#-snapshot:.*$/-snapshot:/" "$file"
        elif grep -q "^-snapshot:" "$file"; then
            "${SED_INPLACE[@]}" "s/^-snapshot:.*$/-snapshot:/" "$file"
        fi
    elif [[ "$snapshot_value" == "SNAPSHOT" ]]; then
        # Development: #-snapshot: (commented out)
        if grep -q "^-snapshot:" "$file"; then
            "${SED_INPLACE[@]}" "s/^-snapshot:.*$/#-snapshot:/" "$file"
        fi
    else
        # RC release: -snapshot: RCx
        if grep -q "^#-snapshot:" "$file"; then
            "${SED_INPLACE[@]}" "s/^#-snapshot:.*$/-snapshot: $snapshot_value/" "$file"
        elif grep -q "^-snapshot:" "$file"; then
            "${SED_INPLACE[@]}" "s/^-snapshot:.*$/-snapshot: $snapshot_value/" "$file"
        fi
    fi
}

# Update About.java
update_about_java() {
    local file="${REPO_ROOT}/biz.aQute.bndlib/src/aQute/bnd/osgi/About.java"
    local new_version=$1
    local update_current=$2  # true or false

    log_info "Updating ${file}"

    # Parse version components
    parse_version "$new_version" "NEW"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would add version constant: _${NEW_MAJOR}_${NEW_MINOR}"
        if [[ "$update_current" == true ]]; then
            log_info "  Would update CURRENT to: _${NEW_MAJOR}_${NEW_MINOR}"
        fi
        return
    fi

    # Check if version constant already exists
    local version_const="_${NEW_MAJOR}_${NEW_MINOR}"
    if ! grep -q "public static final Version.*${version_const}[[:space:]]*=" "$file"; then
        # Find the line with CURRENT and add the new version before it
        # First, find the last version constant line (the one before CURRENT)
        local last_version_line
        last_version_line=$(grep -n "public static final Version.*_[0-9]*_[0-9]*[[:space:]]*=" "$file" | grep -v "CURRENT" | tail -1 | cut -d: -f1)

        if [[ -n "$last_version_line" ]]; then
            # Insert the new version after the last version constant
            # Use a temp file approach for portability across GNU and BSD sed
            local new_line="	public static final Version					${version_const}		= new Version(${NEW_MAJOR}, ${NEW_MINOR}, 0);"
            local tmpfile
            tmpfile=$(mktemp)
            awk -v line="$last_version_line" -v text="$new_line" 'NR==line {print; print text; next} 1' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
        fi
    fi

    if [[ "$update_current" == true ]]; then
        # Update CURRENT to point to the new version
        "${SED_INPLACE[@]}" "s/public static final Version[[:space:]]*CURRENT[[:space:]]*=.*/public static final Version					CURRENT		= ${version_const};/" "$file"

        # Check if CHANGES constant exists, if not add it
        local changes_const="CHANGES_${NEW_MAJOR}_${NEW_MINOR}"
        if ! grep -q "public static final String\[\].*${changes_const}[[:space:]]*=" "$file"; then
            # Find the first CHANGES line and add the new one before it
            local first_changes_line
            first_changes_line=$(grep -n "public static final String\[\].*CHANGES_[0-9]*_[0-9]*[[:space:]]*=" "$file" | head -1 | cut -d: -f1)

            if [[ -n "$first_changes_line" ]]; then
                # Use a temp file approach for portability
                local tmpfile
                tmpfile=$(mktemp)
                {
                    head -n $((first_changes_line - 1)) "$file"
                    echo "	public static final String[]				${changes_const}	= {"
                    echo "		\"See https://github.com/bndtools/bnd/wiki/Changes-in-${new_version} for a list of changes.\""
                    echo "	};"
                    tail -n +"$first_changes_line" "$file"
                } > "$tmpfile" && mv "$tmpfile" "$file"
            fi
        fi

        # Update CHANGES map to include the new version (add at the beginning after the comment)
        # The format is: Maps.ofEntries( followed by // In decreasing order comment, then entries
        if ! grep -q "Maps.entry(${version_const}, ${changes_const})" "$file"; then
            # Find the line with "In decreasing order" comment
            local comment_line
            comment_line=$(grep -n "// In decreasing order" "$file" | head -1 | cut -d: -f1)
            if [[ -n "$comment_line" ]]; then
                # Add the new entry after the comment line using awk
                local new_entry="		Maps.entry(${version_const}, ${changes_const}),																																							//"
                local tmpfile
                tmpfile=$(mktemp)
                awk -v line="$comment_line" -v text="$new_entry" 'NR==line {print; print text; next} 1' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
            fi
        fi
    fi
}

# Update About.java for patch releases (with MICRO version component)
update_about_java_patch() {
    local file="${REPO_ROOT}/biz.aQute.bndlib/src/aQute/bnd/osgi/About.java"
    local new_version=$1
    local update_current=$2  # true or false

    log_info "Updating ${file} for patch release"

    # Parse version components
    parse_version "$new_version" "NEW"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would add version constant: _${NEW_MAJOR}_${NEW_MINOR}_${NEW_PATCH}"
        if [[ "$update_current" == true ]]; then
            log_info "  Would update CURRENT to: _${NEW_MAJOR}_${NEW_MINOR}_${NEW_PATCH}"
        fi
        return
    fi

    # Check if version constant already exists
    local version_const="_${NEW_MAJOR}_${NEW_MINOR}_${NEW_PATCH}"
    if ! grep -q "public static final Version.*${version_const}[[:space:]]*=" "$file"; then
        # Find the line with CURRENT and add the new version before it
        # First, find the last version constant line (the one before CURRENT)
        # Match both 2-component and 3-component version patterns
        local last_version_line
        last_version_line=$(grep -n "public static final Version.*_[0-9]*_[0-9]*\(_[0-9]*\)\?[[:space:]]*=" "$file" | grep -v "CURRENT" | tail -1 | cut -d: -f1)

        if [[ -n "$last_version_line" ]]; then
            # Insert the new version after the last version constant
            # Use a temp file approach for portability across GNU and BSD sed
            local new_line="	public static final Version					${version_const}		= new Version(${NEW_MAJOR}, ${NEW_MINOR}, ${NEW_PATCH});"
            local tmpfile
            tmpfile=$(mktemp)
            awk -v line="$last_version_line" -v text="$new_line" 'NR==line {print; print text; next} 1' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
        fi
    fi

    if [[ "$update_current" == true ]]; then
        # Update CURRENT to point to the new version
        "${SED_INPLACE[@]}" "s/public static final Version[[:space:]]*CURRENT[[:space:]]*=.*/public static final Version					CURRENT		= ${version_const};/" "$file"

        # Check if CHANGES constant exists, if not add it
        local changes_const="CHANGES_${NEW_MAJOR}_${NEW_MINOR}_${NEW_PATCH}"
        if ! grep -q "public static final String\[\].*${changes_const}[[:space:]]*=" "$file"; then
            # Find the first CHANGES line and add the new one before it
            # Match both 2-component and 3-component CHANGES patterns
            local first_changes_line
            first_changes_line=$(grep -n "public static final String\[\].*CHANGES_[0-9]*_[0-9]*\(_[0-9]*\)\?[[:space:]]*=" "$file" | head -1 | cut -d: -f1)

            if [[ -n "$first_changes_line" ]]; then
                # Use a temp file approach for portability
                local tmpfile
                tmpfile=$(mktemp)
                {
                    head -n $((first_changes_line - 1)) "$file"
                    echo "	public static final String[]				${changes_const}	= {"
                    echo "		\"See https://github.com/bndtools/bnd/wiki/Changes-in-${new_version} for a list of changes.\""
                    echo "	};"
                    tail -n +"$first_changes_line" "$file"
                } > "$tmpfile" && mv "$tmpfile" "$file"
            fi
        fi

        # Update CHANGES map to include the new version (add at the beginning after the comment)
        # The format is: Maps.ofEntries( followed by // In decreasing order comment, then entries
        if ! grep -q "Maps.entry(${version_const}, ${changes_const})" "$file"; then
            # Find the line with "In decreasing order" comment
            local comment_line
            comment_line=$(grep -n "// In decreasing order" "$file" | head -1 | cut -d: -f1)
            if [[ -n "$comment_line" ]]; then
                # Add the new entry after the comment line using awk
                local new_entry="		Maps.entry(${version_const}, ${changes_const}),																																							//"
                local tmpfile
                tmpfile=$(mktemp)
                awk -v line="$comment_line" -v text="$new_entry" 'NR==line {print; print text; next} 1' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
            fi
        fi
    fi
}

# Update package-info.java
update_package_info() {
    local file="${REPO_ROOT}/biz.aQute.bndlib/src/aQute/bnd/osgi/package-info.java"
    local version=$1

    log_info "Updating ${file}"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would set @Version to: $version"
        return
    fi

    "${SED_INPLACE[@]}" "s/@Version(\"[^\"]*\")/@Version(\"$version\")/" "$file"
}

# Update maven-plugins/bnd-plugin-parent/pom.xml
update_maven_pom() {
    local file="${REPO_ROOT}/maven-plugins/bnd-plugin-parent/pom.xml"
    local version=$1

    log_info "Updating ${file}"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would set revision to: $version"
        return
    fi

    "${SED_INPLACE[@]}" "s/<revision>[^<]*<\/revision>/<revision>$version<\/revision>/" "$file"
}

# Update gradle-plugins/gradle.properties
update_gradle_plugins_properties() {
    local file="${REPO_ROOT}/gradle-plugins/gradle.properties"
    local version=$1

    log_info "Updating ${file}"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would set bnd_version to: $version"
        return
    fi

    # Note: gradle-plugins/gradle.properties uses colon separator (bnd_version: value)
    "${SED_INPLACE[@]}" "s/^bnd_version:.*$/bnd_version: $version/" "$file"
}

# Update gradle.properties (root)
# Note: Root gradle.properties uses equals separator (bnd_version=value), different from gradle-plugins/gradle.properties
update_root_gradle_properties() {
    local file="${REPO_ROOT}/gradle.properties"
    local version=$1

    log_info "Updating ${file}"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would set bnd_version to: $version"
        return
    fi

    # Note: Root gradle.properties uses equals separator (bnd_version=value)
    "${SED_INPLACE[@]}" "s/^bnd_version=.*$/bnd_version=$version/" "$file"
}

# Update gradle-plugins/README.md
# Only update specific version references, not example versions (e.g., milestone examples)
update_gradle_readme() {
    local file="${REPO_ROOT}/gradle-plugins/README.md"
    local old_version=$1
    local new_version=$2

    log_info "Updating ${file}"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would replace version $old_version with $new_version in main examples"
        return
    fi

    # Only replace the actual version references (those matching the old version pattern)
    # This preserves milestone/RC example versions like "6.0.0-M1"
    "${SED_INPLACE[@]}" "s/\"biz.aQute.bnd.builder\" version \"${old_version}\"/\"biz.aQute.bnd.builder\" version \"${new_version}\"/g" "$file"
    "${SED_INPLACE[@]}" "s/\"biz.aQute.bnd.workspace\" version \"${old_version}\"/\"biz.aQute.bnd.workspace\" version \"${new_version}\"/g" "$file"
}

# Create version defaults .bnd file
create_version_bnd() {
    local version=$1
    local file="${REPO_ROOT}/biz.aQute.bndlib/src/aQute/bnd/build/${version}.bnd"

    log_info "Creating ${file}"

    if [[ "$DRY_RUN" == true ]]; then
        log_info "  Would create file with __versiondefaults__ = $version"
        return
    fi

    if [[ -f "$file" ]]; then
        log_warn "File already exists: $file"
        return
    fi

    cat > "$file" << EOF
__versiondefaults__                 $version
-launcher                           manage = all
-jpms-multi-release                 true
EOF
}

# Get current version from cnf/build.bnd
get_current_version() {
    grep "^base\.version:" "${REPO_ROOT}/cnf/build.bnd" | sed 's/base\.version:[[:space:]]*//'
}

# Mode: first-rc
# Prepare master for V2 and next branch for V1.RC1
do_first_rc() {
    log_info "=========================================="
    log_info "Mode: First Release Candidate"
    log_info "Release Version (V1): $RELEASE_VERSION"
    log_info "Next Version (V2): $NEXT_VERSION"
    log_info "RC Number: $RC_NUMBER"
    log_info "=========================================="

    echo ""
    log_info "Step 1: Preparing master branch for V2 ($NEXT_VERSION)"
    log_info "------------------------------------------"

    # Get the current version in the README
    local readme_current_version
    readme_current_version=$(grep -o '"biz.aQute.bnd.builder" version "[^"]*"' "${REPO_ROOT}/gradle-plugins/README.md" | head -1 | sed 's/.*version "\([^"]*\)".*/\1/')

    # Update master branch files for V2
    update_build_bnd "$NEXT_VERSION" "SNAPSHOT"
    update_about_java "$NEXT_VERSION" true
    update_package_info "$NEXT_VERSION"
    update_maven_pom "${NEXT_VERSION}-SNAPSHOT"
    update_gradle_plugins_properties "${NEXT_VERSION}-SNAPSHOT"
    update_gradle_readme "$readme_current_version" "$NEXT_VERSION"
    create_version_bnd "$NEXT_VERSION"

    echo ""
    log_info "Master branch changes complete."
    log_info ""
    log_info "Next steps for master branch:"
    log_info "  1. Review changes with: git diff"
    log_info "  2. Commit: git add . && git commit -m 'build: Start $NEXT_VERSION stream'"
    log_info "  3. Tag: git tag -s ${NEXT_VERSION}.DEV"
    log_info "  4. Build and verify:"
    log_info "       ./gradlew clean :gradle-plugins:clean"
    log_info "       ./mvnw --file=maven-plugins clean"
    log_info "       ./gradlew :build"
    log_info "       ./gradlew :gradle-plugins:build"
    log_info "       ./mvnw --file=maven-plugins install"
    log_info "  5. Push: git push origin master ${NEXT_VERSION}.DEV"
    log_info ""
    log_info "After pushing master, prepare the 'next' branch:"
    log_info "  git checkout next"
    log_info "  $(basename "$0") --mode prepare-next --release-version $RELEASE_VERSION --next-version $NEXT_VERSION --rc $RC_NUMBER"
}

# Mode: prepare-next
# Prepare next branch for first release candidate (V1.RC1)
do_prepare_next() {
    log_info "=========================================="
    log_info "Mode: Prepare Next Branch for RC"
    log_info "Release Version (V1): $RELEASE_VERSION"
    log_info "Next Version (V2): $NEXT_VERSION"
    log_info "RC Number: $RC_NUMBER"
    log_info "=========================================="

    echo ""
    log_info "Preparing next branch for V1.RC$RC_NUMBER ($RELEASE_VERSION-RC$RC_NUMBER)"
    log_info "------------------------------------------"

    # Update root gradle.properties with version range
    update_root_gradle_properties "[${RELEASE_VERSION}-RC,${NEXT_VERSION})"

    # Update -snapshot in build.bnd
    update_build_bnd "$RELEASE_VERSION" "RC$RC_NUMBER"

    # Update maven pom
    update_maven_pom "${RELEASE_VERSION}-RC$RC_NUMBER"

    # Update gradle-plugins/gradle.properties
    update_gradle_plugins_properties "${RELEASE_VERSION}-RC$RC_NUMBER"

    echo ""
    log_info "Changes complete."
    log_info ""
    log_info "Next steps:"
    log_info "  1. Review changes with: git diff"
    log_info "  2. Commit: git add . && git commit -m 'build: Build Release ${RELEASE_VERSION}.RC$RC_NUMBER'"
    log_info "  3. Tag: git tag -s ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info "  4. Push: git push --force origin next ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info ""
    log_info "After pushing, update JFROG manually (see release process documentation)."
    log_info ""
    log_info "To see changes since last release:"
    log_info "  git shortlog ${RELEASE_VERSION}.DEV..next --no-merges"
}

# Mode: next-rc
# Update next branch from RCx to RC(x+1)
do_next_rc() {
    log_info "=========================================="
    log_info "Mode: Next Release Candidate"
    log_info "Release Version: $RELEASE_VERSION"
    log_info "New RC Number: $RC_NUMBER"
    log_info "=========================================="

    local prev_rc=$((RC_NUMBER - 1))

    echo ""
    log_info "Updating next branch from RC$prev_rc to RC$RC_NUMBER"
    log_info "------------------------------------------"

    # Update -snapshot in build.bnd
    update_build_bnd "$RELEASE_VERSION" "RC$RC_NUMBER"

    # Update maven pom
    update_maven_pom "${RELEASE_VERSION}-RC$RC_NUMBER"

    # Update gradle-plugins/gradle.properties
    update_gradle_plugins_properties "${RELEASE_VERSION}-RC$RC_NUMBER"

    echo ""
    log_info "Changes complete."
    log_info ""
    log_info "Next steps:"
    log_info "  1. Review changes with: git diff"
    log_info "  2. Commit: git add . && git commit -m 'build: Build Release ${RELEASE_VERSION}.RC$RC_NUMBER'"
    log_info "  3. Tag: git tag -s ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info "  4. Push: git push origin next ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info ""
    log_info "After pushing, update JFROG manually (see release process documentation)."
    log_info ""
    log_info "To see changes since last release:"
    log_info "  git shortlog ${RELEASE_VERSION}.DEV..next --no-merges"
}

# Mode: release
# Update next branch from RCx to final release
do_release() {
    log_info "=========================================="
    log_info "Mode: Final Release"
    log_info "Release Version: $RELEASE_VERSION"
    log_info "=========================================="

    echo ""
    log_info "Updating next branch for final release"
    log_info "------------------------------------------"

    # Update -snapshot in build.bnd (empty value for release)
    update_build_bnd "$RELEASE_VERSION" ""

    # Update maven pom (no qualifier)
    update_maven_pom "$RELEASE_VERSION"

    # Update gradle-plugins/gradle.properties (no qualifier)
    update_gradle_plugins_properties "$RELEASE_VERSION"

    echo ""
    log_info "Changes complete."
    log_info ""
    log_info "Next steps:"
    log_info "  1. Review changes with: git diff"
    log_info "  2. Commit: git add . && git commit -m 'build: Build Release $RELEASE_VERSION'"
    log_info "  3. Tag: git tag -s $RELEASE_VERSION"
    log_info "  4. Push: git push origin next $RELEASE_VERSION"
    log_info ""
    log_info "After the push, the release will be built and released into libs-release-local."
}

# Mode: patch-first-rc
# Prepare next branch for first patch release candidate (V1.RC1)
# This is used for MICRO version releases (e.g., 7.2.1 after 7.2.0)
do_patch_first_rc() {
    log_info "=========================================="
    log_info "Mode: First Patch Release Candidate"
    log_info "Release Version (V1): $RELEASE_VERSION"
    log_info "Next Version (V2): $NEXT_VERSION"
    log_info "RC Number: $RC_NUMBER"
    log_info "=========================================="

    echo ""
    log_info "Preparing next branch for patch V1.RC$RC_NUMBER ($RELEASE_VERSION-RC$RC_NUMBER)"
    log_info "------------------------------------------"

    # Parse version to get the base version (previous final release)
    parse_version "$RELEASE_VERSION" "REL"
    local base_version="${REL_MAJOR}.${REL_MINOR}.0"
    
    log_info "Base version for RC1: $base_version"

    # Update root gradle.properties with version range starting from base version
    # For RC1 of a patch release, we use the previous final release (e.g., 7.2.0 for 7.2.1-RC1)
    update_root_gradle_properties "[$base_version,$NEXT_VERSION)"

    # Update -snapshot in build.bnd
    update_build_bnd "$RELEASE_VERSION" "RC$RC_NUMBER"

    # Update About.java with patch version constant
    update_about_java_patch "$RELEASE_VERSION" true

    # Update package-info.java
    update_package_info "$RELEASE_VERSION"

    # Update maven pom
    update_maven_pom "${RELEASE_VERSION}-RC$RC_NUMBER"

    # Update gradle-plugins/gradle.properties
    update_gradle_plugins_properties "${RELEASE_VERSION}-RC$RC_NUMBER"

    # Get the current version in the README
    local readme_current_version
    readme_current_version=$(grep -o '"biz.aQute.bnd.builder" version "[^"]*"' "${REPO_ROOT}/gradle-plugins/README.md" | head -1 | sed 's/.*version "\([^"]*\)".*/\1/')

    # Update gradle README if base version is different from current
    if [[ "$readme_current_version" != "$RELEASE_VERSION" ]]; then
        update_gradle_readme "$readme_current_version" "$RELEASE_VERSION"
    fi

    # Create version defaults .bnd file
    create_version_bnd "$RELEASE_VERSION"

    echo ""
    log_info "Changes complete."
    log_info ""
    log_info "Next steps:"
    log_info "  1. Review changes with: git diff"
    log_info "  2. Commit: git add . && git commit -m 'build: Build Release ${RELEASE_VERSION}.RC$RC_NUMBER'"
    log_info "  3. Tag: git tag -s ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info "  4. Push: git push --force origin next ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info ""
    log_info "After pushing, update JFROG manually (see release process documentation)."
    log_info ""
    log_info "IMPORTANT: For patch RC1, gradle.properties uses [$base_version,$NEXT_VERSION) to build with the previous release."
    log_info "           For subsequent RCs (RC2+), use patch-next-rc mode which will update to [${RELEASE_VERSION}-RC,$NEXT_VERSION)."
}

# Mode: patch-next-rc
# Update next branch from RCx to RC(x+1) for patch releases
do_patch_next_rc() {
    log_info "=========================================="
    log_info "Mode: Next Patch Release Candidate"
    log_info "Release Version: $RELEASE_VERSION"
    log_info "Next Version: $NEXT_VERSION"
    log_info "New RC Number: $RC_NUMBER"
    log_info "=========================================="

    local prev_rc=$((RC_NUMBER - 1))

    echo ""
    log_info "Updating next branch from RC$prev_rc to RC$RC_NUMBER for patch release"
    log_info "------------------------------------------"

    # Update root gradle.properties with version range
    # For RC2+, we use the RC range to pick up previous RCs
    update_root_gradle_properties "[${RELEASE_VERSION}-RC,${NEXT_VERSION})"

    # Update -snapshot in build.bnd
    update_build_bnd "$RELEASE_VERSION" "RC$RC_NUMBER"

    # Update maven pom
    update_maven_pom "${RELEASE_VERSION}-RC$RC_NUMBER"

    # Update gradle-plugins/gradle.properties
    update_gradle_plugins_properties "${RELEASE_VERSION}-RC$RC_NUMBER"

    echo ""
    log_info "Changes complete."
    log_info ""
    log_info "Next steps:"
    log_info "  1. Review changes with: git diff"
    log_info "  2. Commit: git add . && git commit -m 'build: Build Release ${RELEASE_VERSION}.RC$RC_NUMBER'"
    log_info "  3. Tag: git tag -s ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info "  4. Push: git push origin next ${RELEASE_VERSION}.RC$RC_NUMBER"
    log_info ""
    log_info "After pushing, update JFROG manually (see release process documentation)."
    log_info ""
    log_info "To see changes since last release:"
    log_info "  git shortlog ${RELEASE_VERSION}.DEV..next --no-merges"
}

# Main
main() {
    validate_args

    cd "$REPO_ROOT"

    case "$MODE" in
        first-rc)
            do_first_rc
            ;;
        prepare-next)
            do_prepare_next
            ;;
        next-rc)
            do_next_rc
            ;;
        release)
            do_release
            ;;
        patch-first-rc)
            do_patch_first_rc
            ;;
        patch-next-rc)
            do_patch_next_rc
            ;;
    esac

    echo ""
    if [[ "$DRY_RUN" == true ]]; then
        log_warn "This was a dry run. No changes were made."
    else
        log_info "Script completed. Please review the changes before committing."
    fi
}

main
