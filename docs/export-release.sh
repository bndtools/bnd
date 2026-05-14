#!/bin/bash
# Export documentation from a specific release folder
#
# This script exports documentation from the releases/ directory structure
# which contains archived documentation for specific versions.
#
# Usage:
#   ./export-release.sh <version> [format]
#
# Arguments:
#   version - The version to export (e.g., 7.0.0, 6.4.0)
#   format  - Optional. Output format: html or pdf (default: html)
#
# Examples:
#   ./export-release.sh 7.0.0          # Export to HTML
#   ./export-release.sh 6.4.0 pdf      # Export to PDF
#   ./export-release.sh 7.1.0 html     # Explicitly export to HTML

set -e

VERSION="$1"
FORMAT="${2:-html}"

if [ -z "$VERSION" ]; then
    echo "Error: Version required"
    echo ""
    echo "Usage: $0 <version> [format]"
    echo ""
    echo "Available versions:"
    ls -1 releases/ 2>/dev/null | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -V || echo "  (none found)"
    exit 1
fi

RELEASE_DIR="releases/$VERSION"

if [ ! -d "$RELEASE_DIR" ]; then
    echo "Error: Release directory not found: $RELEASE_DIR"
    echo ""
    echo "Available versions:"
    ls -1 releases/ 2>/dev/null | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -V || echo "  (none found)"
    exit 1
fi

echo "=========================================="
echo "Bnd Documentation Release Export"
echo "=========================================="
echo ""
echo "Version: $VERSION"
echo "Format:  $FORMAT"
echo "Source:  $RELEASE_DIR"
echo ""

# Export using the release directory as _site
case "$FORMAT" in
    html)
        OUTPUT_FILE="bnd-docs-${VERSION}.html"
        SITE_DIR="$RELEASE_DIR" OUTPUT_FILE="$OUTPUT_FILE" VERSION="$VERSION" python3 << 'PYTHON_SCRIPT'
import os
import sys

# Import the export logic from the main script
# For now, just provide a message
print("To export release documentation:")
print("1. Temporarily copy the release folder to _site:")
print(f"   cp -r {os.environ['SITE_DIR']} _site")
print("2. Run the export script:")
print(f"   ./export-single-html.sh {os.environ['VERSION']}")
print("3. Clean up:")
print("   rm -rf _site")
sys.exit(0)
PYTHON_SCRIPT
        
        echo "Creating export from $RELEASE_DIR..."
        
        # Create a temporary symlink or copy
        if [ -d "_site" ]; then
            echo "Warning: _site directory exists. Please remove it first or backup."
            exit 1
        fi
        
        # Create symlink to release directory
        ln -s "$RELEASE_DIR" _site
        
        # Run the export
        ./export-single-html.sh "$VERSION" "bnd-docs-${VERSION}.html"
        
        # Remove symlink
        rm _site
        
        echo ""
        echo "✓ Export complete: bnd-docs-${VERSION}.html"
        ;;
        
    pdf)
        OUTPUT_FILE="bnd-docs-${VERSION}.pdf"
        
        echo "Creating export from $RELEASE_DIR..."
        
        # Create a temporary symlink
        if [ -d "_site" ]; then
            echo "Warning: _site directory exists. Please remove it first or backup."
            exit 1
        fi
        
        ln -s "$RELEASE_DIR" _site
        
        # Run the export
        ./export-pdf.sh "$VERSION" "bnd-docs-${VERSION}.pdf"
        
        # Remove symlink
        rm _site
        
        echo ""
        echo "✓ Export complete: bnd-docs-${VERSION}.pdf"
        ;;
        
    *)
        echo "Error: Invalid format '$FORMAT'. Use 'html' or 'pdf'."
        exit 1
        ;;
esac
