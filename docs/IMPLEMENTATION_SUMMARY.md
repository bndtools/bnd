# Documentation Export Implementation Summary

This document summarizes the implementation of HTML and PDF export functionality for the Bnd documentation.

## Problem Statement

Users requested options for creating single HTML pages or PDF files from the Bnd documentation folder for specific versions. This is useful for:
- Offline documentation access
- Archiving documentation snapshots
- Distributing documentation without a web server
- Regulatory compliance and record-keeping

## Solution Implemented

### 1. Single HTML Export Script (`export-single-html.sh`)

**Purpose:** Combines all documentation pages into one self-contained HTML file.

**Features:**
- Extracts main content from Jekyll-generated HTML pages
- Generates a table of contents with clickable links
- Includes print-friendly CSS styles
- Supports both `_site` directory (from fresh builds) and `releases/` directory structures
- Handles different HTML layouts (with and without `data-pagefind-body` markers)

**Usage:**
```bash
./export-single-html.sh [version] [output-file]
```

**Technical Details:**
- Written in Bash with embedded Python for HTML processing
- Uses Python's `HTMLParser` to extract content from Jekyll pages
- Collects pages in logical order (index, introduction, chapters, instructions, macros, commands, headers, tools, plugins)
- Generates responsive HTML with embedded CSS
- Output is completely self-contained (no external dependencies)

### 2. PDF Export Script (`export-pdf.sh`)

**Purpose:** Converts documentation to PDF format.

**Features:**
- Auto-detects available PDF generation tools (wkhtmltopdf, weasyprint, chromium)
- Creates single HTML first, then converts to PDF
- Includes print-optimized formatting
- Handles missing tools gracefully (provides fallback instructions)
- Supports multiple PDF engines with appropriate command-line options

**Usage:**
```bash
./export-pdf.sh [version] [output-file]
```

**Supported PDF Tools:**
1. **wkhtmltopdf** (recommended) - Best quality output
2. **weasyprint** - Python-based, good for CI/CD
3. **Chromium/Chrome** - Widely available, headless mode
4. **Manual** - Export HTML and use browser's "Print to PDF"

**Technical Details:**
- Automatically selects best available PDF tool
- Configures tool-specific options for optimal output
- Cleans up temporary files after conversion
- Provides file size and page count in output

### 3. Release Export Script (`export-release.sh`)

**Purpose:** Export documentation from archived releases in the `releases/` directory.

**Features:**
- Works with pre-built release documentation
- Doesn't require rebuilding Jekyll site
- Supports both HTML and PDF output formats
- Lists available release versions

**Usage:**
```bash
./export-release.sh <version> [format]
# Examples:
./export-release.sh 7.0.0          # Export to HTML
./export-release.sh 7.0.0 pdf      # Export to PDF
```

**Technical Details:**
- Creates temporary symlink from `releases/X.Y.Z` to `_site`
- Calls appropriate export script
- Cleans up symlink after export
- Handles different directory structures in releases

### 4. Documentation (`EXPORT_README.md`)

**Purpose:** Comprehensive guide for using the export functionality.

**Contents:**
- Quick start guide
- Installation instructions for PDF tools
- Usage examples for all scripts
- Troubleshooting guide
- Examples of output files
- CI/CD integration examples

### 5. Updated Main README

Added section linking to export functionality and providing quick examples.

## Implementation Highlights

### HTML Content Extraction

The most complex part was extracting main content from different HTML structures:

1. **_site structure**: Uses `<main data-pagefind-body>` tags
2. **Release structure**: Uses `<div class="notes-margin">` for content
3. **Navigation/menus**: Different structures need to be filtered out

Solution: Simplified parser that looks for main content markers and captures everything within, using depth tracking to properly close tags.

### Directory Structure Handling

The scripts support two directory structures:
- `_site/_chapters/`, `_site/_instructions/`, etc. (from Jekyll build)
- `_site/chapters/`, `_site/instructions/`, etc. (from releases)

Solution: Check for both underscore-prefixed and non-prefixed directories, avoiding duplicates.

### PDF Generation Flexibility

Different environments have different PDF tools available.

Solution: Auto-detection of available tools with fallback graceful degradation to HTML-only export with manual conversion instructions.

## Testing

Tested with:
- ✅ Fresh Jekyll builds (minimal test structure)
- ✅ Release 7.0.0 documentation (443 pages)
- ✅ HTML export (1.7 MB output)
- ✅ PDF export via Chromium (995 KB output)
- ✅ Both _site and releases directory structures

## Files Modified/Created

### New Files:
1. `docs/export-single-html.sh` - Main HTML export script
2. `docs/export-pdf.sh` - PDF export script  
3. `docs/export-release.sh` - Release-specific export script
4. `docs/EXPORT_README.md` - Comprehensive documentation

### Modified Files:
1. `docs/README.md` - Added export functionality section
2. `docs/.gitignore` - Added patterns to ignore generated export files

## Usage Examples

### Export Current Development Version
```bash
cd docs
./build.sh                    # Build Jekyll site
./export-single-html.sh       # Create bnd-docs-master.html
./export-pdf.sh               # Create bnd-docs-master.pdf
```

### Export Archived Release
```bash
cd docs
./export-release.sh 7.0.0 html  # Create bnd-docs-7.0.0.html
./export-release.sh 7.0.0 pdf   # Create bnd-docs-7.0.0.pdf
```

### Export Specific Version from Git
```bash
cd /path/to/bnd
git checkout 7.0.0
cd docs
./build.sh
./export-single-html.sh 7.0.0
./export-pdf.sh 7.0.0
```

## Future Enhancements (Not Implemented)

Potential improvements for the future:
1. Add bookmarks/outline to PDF files
2. Support for custom CSS themes
3. Filtering specific sections (e.g., only commands, only instructions)
4. Version comparison exports
5. Incremental updates (only changed pages)
6. Integration with GitHub Actions for automatic release exports

## Conclusion

The implementation provides a complete solution for exporting Bnd documentation to offline formats. The scripts are flexible, handle multiple scenarios, and provide good error handling and user feedback. Users can now easily create single HTML or PDF files for any version of the documentation, whether from the current development branch, a specific git tag, or archived releases.
