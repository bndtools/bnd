# Documentation Export - Usage Examples

This document provides practical examples of using the documentation export scripts.

## Prerequisites

Ensure you have the bnd documentation repository cloned:
```bash
git clone https://github.com/bndtools/bnd.git
cd bnd/docs
```

## Example 1: Export Master Branch Documentation

Export the latest development documentation:

```bash
# Build the documentation
./build.sh

# Export to single HTML file
./export-single-html.sh master

# Export to PDF  
./export-pdf.sh master

# Output files:
# - bnd-docs-master.html (~2-3 MB)
# - bnd-docs-master.pdf (~1-2 MB)
```

## Example 2: Export a Specific Version

Export documentation for version 7.0.0 from git:

```bash
# Checkout the version tag
cd .. && git checkout 7.0.0 && cd docs

# Build the documentation for this version
./build.sh

# Export
./export-single-html.sh 7.0.0 bnd-docs-7.0.0.html
./export-pdf.sh 7.0.0 bnd-docs-7.0.0.pdf

# Return to master
cd .. && git checkout master && cd docs
```

## Example 3: Export from Archived Release

The easiest way to export an archived release (no git checkout needed):

```bash
# List available releases
ls -1 releases/

# Export version 7.0.0 to HTML
./export-release.sh 7.0.0

# Export version 7.0.0 to PDF
./export-release.sh 7.0.0 pdf

# Export version 6.4.0
./export-release.sh 6.4.0 html

# Output files:
# - bnd-docs-7.0.0.html
# - bnd-docs-7.0.0.pdf  
# - bnd-docs-6.4.0.html
```

## Example 4: Export Multiple Versions for Comparison

Create exports for multiple versions:

```bash
# Export several releases
for version in 6.4.0 7.0.0 7.1.0 7.2.0; do
    echo "Exporting version $version..."
    ./export-release.sh $version html
done

# Now you have:
# - bnd-docs-6.4.0.html
# - bnd-docs-7.0.0.html
# - bnd-docs-7.1.0.html
# - bnd-docs-7.2.0.html
```

## Example 5: Custom Output Location

Specify custom output file names and locations:

```bash
# Build and export to custom location
./build.sh
./export-single-html.sh master /tmp/bnd-documentation.html
./export-pdf.sh master /tmp/bnd-documentation.pdf

# Or just custom name in current directory
./export-single-html.sh master my-bnd-docs.html
```

## Example 6: View the Generated Documentation

### HTML:
```bash
# Linux
xdg-open bnd-docs-master.html

# macOS
open bnd-docs-master.html

# Windows
start bnd-docs-master.html

# Or any web browser
firefox bnd-docs-master.html
google-chrome bnd-docs-master.html
```

### PDF:
```bash
# Linux
xdg-open bnd-docs-master.pdf

# macOS
open bnd-docs-master.pdf

# Windows
start bnd-docs-master.pdf

# Or any PDF viewer
evince bnd-docs-master.pdf  # Linux
```

## Example 7: Installing PDF Generation Tools

### Ubuntu/Debian:
```bash
# wkhtmltopdf (recommended)
sudo apt-get install wkhtmltopdf

# weasyprint
sudo apt-get install python3-pip
pip3 install weasyprint

# chromium
sudo apt-get install chromium-browser
```

### macOS:
```bash
# wkhtmltopdf
brew install wkhtmltopdf

# weasyprint
pip3 install weasyprint

# chromium/chrome (usually already installed)
brew install --cask google-chrome
```

### Manual PDF Creation:
If you don't want to install PDF tools:
```bash
# Create HTML only
./export-single-html.sh 7.0.0

# Open in browser
open bnd-docs-7.0.0.html

# Use browser's Print function:
# - Press Cmd+P (Mac) or Ctrl+P (Windows/Linux)
# - Select "Save as PDF"
# - Click Save
```

## Example 8: Integration with CI/CD

### GitHub Actions:
```yaml
name: Export Documentation

on:
  release:
    types: [published]

jobs:
  export-docs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.2'
          
      - name: Build documentation
        run: |
          cd docs
          bundle install
          bundle exec jekyll build
          
      - name: Install PDF tools
        run: sudo apt-get install -y wkhtmltopdf
        
      - name: Export to HTML and PDF
        run: |
          cd docs
          ./export-single-html.sh ${{ github.ref_name }}
          ./export-pdf.sh ${{ github.ref_name }}
          
      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: documentation-exports
          path: |
            docs/bnd-docs-*.html
            docs/bnd-docs-*.pdf
```

### Manual Archive Creation:
```bash
# Export all major versions and create an archive
mkdir -p exported-docs
for version in 6.4.0 7.0.0 7.1.0 7.2.0; do
    ./export-release.sh $version html
    ./export-release.sh $version pdf
    mv bnd-docs-${version}.* exported-docs/
done

# Create archive
tar -czf bnd-documentation-archive-$(date +%Y%m%d).tar.gz exported-docs/
```

## Example 9: Troubleshooting

### Issue: "_site directory not found"
```bash
# Solution: Build the site first
./build.sh
```

### Issue: "No PDF generation tool found"
```bash
# Solution 1: Install a tool
sudo apt-get install wkhtmltopdf

# Solution 2: Create HTML and convert manually
./export-single-html.sh master
# Then open in browser and use Print to PDF
```

### Issue: "Permission denied" when running scripts
```bash
# Solution: Make scripts executable
chmod +x export-*.sh
```

### Issue: PDF looks different from website
This is intentional! The PDF uses print-optimized styles for better readability when printed or viewed in PDF readers. Interactive features like search and navigation are removed for the static format.

## Example 10: Verifying Output

Check the generated files:

```bash
# List exported files
ls -lh bnd-docs-*

# Check HTML file size and structure
file bnd-docs-master.html
wc -l bnd-docs-master.html

# Check PDF file info
file bnd-docs-master.pdf
pdfinfo bnd-docs-master.pdf  # If pdfinfo is installed

# Preview first few lines of HTML
head -50 bnd-docs-master.html
```

## Tips

1. **For offline use**: Export to HTML - it's self-contained and searchable with Ctrl+F
2. **For printing**: Export to PDF - better page breaks and formatting
3. **For archiving**: Create both formats with version in filename
4. **For CI/CD**: Use weasyprint (pure Python, easier to install in containers)
5. **For best quality**: Use wkhtmltopdf (produces best-looking PDFs)

## Next Steps

- Read the full documentation in [EXPORT_README.md](EXPORT_README.md)
- Check implementation details in [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- Report issues or suggest improvements on GitHub
