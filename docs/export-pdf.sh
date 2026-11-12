#!/bin/bash
# Export documentation to PDF
# 
# This script converts the documentation to PDF format using one of several
# available tools. The script will automatically detect which tool is available.
#
# Usage:
#   ./export-pdf.sh [version] [output-file]
#
# Arguments:
#   version      - Optional. The version to export (default: master)
#   output-file  - Optional. Output file path (default: bnd-docs-{version}.pdf)
#
# Examples:
#   ./export-pdf.sh                    # Export master branch
#   ./export-pdf.sh 7.0.0              # Export version 7.0.0
#   ./export-pdf.sh master custom.pdf  # Custom output file
#
# Prerequisites:
#   - The Jekyll site must be built first (run ./build.sh)
#   - One of the following PDF generation tools:
#     * wkhtmltopdf (recommended) - https://wkhtmltopdf.org/
#     * weasyprint - pip install weasyprint
#     * Prince - https://www.princexml.com/ (commercial)
#     * Chromium/Chrome with headless mode
#
# If no PDF tool is available, this script will:
#   1. First create a single HTML file (using export-single-html.sh)
#   2. Print instructions for manual PDF generation

set -e

# Configuration
VERSION="${1:-master}"
OUTPUT_FILE="${2:-bnd-docs-${VERSION}.pdf}"
TEMP_HTML="bnd-docs-temp-${VERSION}.html"

echo "=========================================="
echo "Bnd Documentation PDF Export"
echo "=========================================="
echo ""
echo "Version: ${VERSION}"
echo "Output:  ${OUTPUT_FILE}"
echo ""

# Check if _site directory exists
if [ ! -d "_site" ]; then
    echo "Error: _site directory not found."
    echo "Please run './build.sh' first to generate the Jekyll site."
    exit 1
fi

# Function to detect PDF generation tool
detect_pdf_tool() {
    if command -v wkhtmltopdf &> /dev/null; then
        echo "wkhtmltopdf"
    elif command -v weasyprint &> /dev/null; then
        echo "weasyprint"
    elif command -v prince &> /dev/null; then
        echo "prince"
    elif command -v chromium-browser &> /dev/null; then
        echo "chromium-browser"
    elif command -v google-chrome &> /dev/null; then
        echo "google-chrome"
    elif command -v chromium &> /dev/null; then
        echo "chromium"
    else
        echo "none"
    fi
}

# Detect available tool
PDF_TOOL=$(detect_pdf_tool)

if [ "$PDF_TOOL" = "none" ]; then
    echo "No PDF generation tool found."
    echo ""
    echo "Creating single HTML file instead..."
    echo "You can then convert it to PDF manually using:"
    echo "  - Print to PDF from your browser"
    echo "  - Or install a PDF tool:"
    echo "    * wkhtmltopdf: sudo apt-get install wkhtmltopdf"
    echo "    * weasyprint: pip install weasyprint"
    echo "    * chromium: sudo apt-get install chromium-browser"
    echo ""
    
    # Create single HTML as fallback
    ./export-single-html.sh "$VERSION" "$TEMP_HTML"
    
    echo ""
    echo "HTML file created: $TEMP_HTML"
    echo "You can now:"
    echo "  1. Open it in a browser and use Print -> Save as PDF"
    echo "  2. Or install a PDF tool and run this script again"
    exit 0
fi

echo "Using PDF tool: $PDF_TOOL"
echo ""

# First, create a single HTML file
echo "Step 1/2: Creating single HTML file..."
./export-single-html.sh "$VERSION" "$TEMP_HTML"

echo ""
echo "Step 2/2: Converting to PDF..."

# Generate PDF based on available tool
case "$PDF_TOOL" in
    wkhtmltopdf)
        wkhtmltopdf \
            --enable-local-file-access \
            --print-media-type \
            --page-size A4 \
            --margin-top 20mm \
            --margin-bottom 20mm \
            --margin-left 15mm \
            --margin-right 15mm \
            --footer-center "Page [page] of [topage]" \
            --footer-font-size 9 \
            "$TEMP_HTML" "$OUTPUT_FILE"
        ;;
    
    weasyprint)
        weasyprint "$TEMP_HTML" "$OUTPUT_FILE"
        ;;
    
    prince)
        prince "$TEMP_HTML" -o "$OUTPUT_FILE"
        ;;
    
    chromium-browser|google-chrome|chromium)
        $PDF_TOOL \
            --headless \
            --disable-gpu \
            --no-sandbox \
            --print-to-pdf="$OUTPUT_FILE" \
            --no-pdf-header-footer \
            "file://$(pwd)/$TEMP_HTML" 2>/dev/null || {
                echo "Warning: Chromium had issues, trying with different options..."
                $PDF_TOOL \
                    --headless=new \
                    --disable-gpu \
                    --no-sandbox \
                    --print-to-pdf="$OUTPUT_FILE" \
                    "file://$(pwd)/$TEMP_HTML" 2>/dev/null
            }
        ;;
esac

# Check if PDF was created successfully
if [ -f "$OUTPUT_FILE" ]; then
    PDF_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
    echo ""
    echo "✓ PDF generated successfully!"
    echo "  Output: $OUTPUT_FILE"
    echo "  Size: $PDF_SIZE"
    echo ""
    
    # Clean up temporary HTML
    rm -f "$TEMP_HTML"
    
    echo "To view the PDF:"
    echo "  xdg-open '$OUTPUT_FILE'  # Linux"
    echo "  open '$OUTPUT_FILE'      # macOS"
else
    echo ""
    echo "Error: PDF generation failed."
    echo "The temporary HTML file is available at: $TEMP_HTML"
    exit 1
fi
