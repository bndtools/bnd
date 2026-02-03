# Exporting Documentation

This directory contains scripts to export the Bnd documentation to single HTML or PDF files for offline viewing, archiving, or distribution.

## Quick Start

### Prerequisites

1. **Build the documentation first:**
   ```bash
   ./build.sh
   ```
   This generates the static HTML site in the `_site` directory.

2. **For PDF export, install a PDF generation tool (optional):**
   - **wkhtmltopdf** (recommended): `sudo apt-get install wkhtmltopdf`
   - **weasyprint**: `pip install weasyprint`
   - **Chromium/Chrome**: Available on most systems
   - Or use browser's "Print to PDF" feature

## Usage

### Export to Single HTML

Create a single, self-contained HTML file with all documentation:

```bash
# Export master branch documentation
./export-single-html.sh

# Export specific version
./export-single-html.sh 7.0.0

# Export with custom output filename
./export-single-html.sh master my-docs.html
```

**Output:** `bnd-docs-{version}.html`

Features:
- Complete table of contents with clickable links
- All documentation in a single file
- Print-friendly CSS styles
- Works offline
- Can be opened in any web browser

### Export to PDF

Create a PDF file from the documentation:

```bash
# Export master branch documentation
./export-pdf.sh

# Export specific version
./export-pdf.sh 7.0.0

# Export with custom output filename
./export-pdf.sh master my-docs.pdf
```

**Output:** `bnd-docs-{version}.pdf`

The script will:
1. Auto-detect available PDF generation tools
2. Create a single HTML file (temporary)
3. Convert it to PDF
4. Clean up temporary files

If no PDF tool is installed, the script will create an HTML file with instructions for manual PDF conversion.

## Exporting Specific Versions

To export documentation for a specific release version:

1. **Switch to the release branch or tag:**
   ```bash
   cd ..  # Go to repository root
   git checkout 7.0.0  # Or any version tag
   cd docs
   ```

2. **Build the documentation for that version:**
   ```bash
   ./build.sh
   ```

3. **Export:**
   ```bash
   ./export-single-html.sh 7.0.0
   ./export-pdf.sh 7.0.0
   ```

## Exporting Release Documentation

For archived releases in the `releases/` folder:

```bash
# If you want to export a specific archived release
# You need to adjust the scripts to point to releases/{version} folder
# Or manually navigate to that folder's HTML
```

## Manual PDF Generation

If you prefer to create PDFs manually:

1. Create the HTML file:
   ```bash
   ./export-single-html.sh
   ```

2. Open the HTML file in your browser:
   ```bash
   open bnd-docs-master.html  # macOS
   xdg-open bnd-docs-master.html  # Linux
   ```

3. Use your browser's Print function:
   - Press `Ctrl+P` (or `Cmd+P` on macOS)
   - Select "Save as PDF" as the destination
   - Adjust page settings if needed
   - Save the PDF

## Advanced Options

### Installing PDF Tools

**wkhtmltopdf (recommended):**
```bash
# Ubuntu/Debian
sudo apt-get install wkhtmltopdf

# macOS
brew install wkhtmltopdf

# From source
https://wkhtmltopdf.org/downloads.html
```

**WeasyPrint:**
```bash
# Using pip
pip install weasyprint

# Ubuntu/Debian (with dependencies)
sudo apt-get install python3-pip python3-cffi python3-brotli libpango-1.0-0 libpangoft2-1.0-0
pip install weasyprint
```

**Chromium/Chrome:**
```bash
# Ubuntu/Debian
sudo apt-get install chromium-browser

# macOS
brew install --cask google-chrome
```

### Customizing the Export

The export scripts use Python to process HTML files. You can customize:

1. **Styling:** Edit the CSS in `export-single-html.sh` (look for the `<style>` section)
2. **Content filtering:** Modify the `ContentExtractor` class to include/exclude specific elements
3. **PDF settings:** Adjust PDF tool command-line options in `export-pdf.sh`

### Troubleshooting

**"_site directory not found"**
- Run `./build.sh` first to generate the Jekyll site

**"No PDF generation tool found"**
- Install one of the recommended PDF tools (see above)
- Or create HTML and use browser's "Print to PDF"

**"Permission denied" when running scripts**
- Make scripts executable: `chmod +x export-*.sh`

**PDF looks different from the website**
- The export uses print-optimized styles
- Some interactive features (search, navigation) are removed
- This is intentional for better PDF readability

**Missing content in export**
- Ensure `./build.sh` completed successfully
- Check that `_site/` directory contains all expected HTML files
- Some pages may be excluded if they're in the Jekyll exclude list

## Output Examples

### Single HTML File
- **Size:** Typically 2-5 MB (varies by version)
- **Format:** Self-contained HTML with embedded CSS
- **Use cases:**
  - Offline documentation viewing
  - Quick searching with browser's find (Ctrl+F)
  - Archiving documentation snapshots
  - Sharing documentation as a single file

### PDF File
- **Size:** Typically 1-3 MB (varies by version and PDF tool)
- **Format:** Standard PDF with table of contents
- **Use cases:**
  - Printing physical documentation
  - E-readers and tablets
  - Regulatory compliance (archival)
  - Annotating documentation

## Continuous Integration

These scripts can be integrated into CI/CD pipelines to automatically generate downloadable documentation:

```yaml
# Example GitHub Actions workflow
- name: Build documentation
  run: |
    cd docs
    ./build.sh

- name: Export to HTML and PDF
  run: |
    cd docs
    ./export-single-html.sh ${{ github.ref_name }}
    ./export-pdf.sh ${{ github.ref_name }}

- name: Upload artifacts
  uses: actions/upload-artifact@v3
  with:
    name: documentation
    path: |
      docs/bnd-docs-*.html
      docs/bnd-docs-*.pdf
```

## License

These export scripts are part of the bnd project and follow the same license (Apache License 2.0).

The exported documentation retains all original copyright and license information from the source material.
