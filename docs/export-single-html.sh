#!/bin/bash
# Export documentation to a single HTML file
# 
# This script combines all documentation pages from the Jekyll _site folder
# into a single HTML file suitable for offline viewing or archiving.
#
# Usage:
#   ./export-single-html.sh [version] [output-file]
#
# Arguments:
#   version      - Optional. The version to export (default: master)
#   output-file  - Optional. Output file path (default: bnd-docs-{version}.html)
#
# Examples:
#   ./export-single-html.sh                    # Export master branch
#   ./export-single-html.sh 7.0.0              # Export version 7.0.0
#   ./export-single-html.sh master custom.html # Custom output file
#
# Prerequisites:
#   - The Jekyll site must be built first (run ./build.sh)
#   - Python 3 must be installed

set -e

# Configuration
VERSION="${1:-master}"
OUTPUT_FILE="${2:-bnd-docs-${VERSION}.html}"
SITE_DIR="_site"

echo "=========================================="
echo "Bnd Documentation Single HTML Export"
echo "=========================================="
echo ""
echo "Version: ${VERSION}"
echo "Output:  ${OUTPUT_FILE}"
echo ""

# Check if _site directory exists
if [ ! -d "${SITE_DIR}" ]; then
    echo "Error: _site directory not found."
    echo "Please run './build.sh' first to generate the Jekyll site."
    exit 1
fi

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo "Error: python3 not found. Please install Python 3."
    exit 1
fi

# Create Python script to combine HTML files
SITE_DIR="${SITE_DIR}" OUTPUT_FILE="${OUTPUT_FILE}" VERSION="${VERSION}" python3 << 'PYTHON_SCRIPT'
import os
import sys
import re
from pathlib import Path
from html.parser import HTMLParser
import datetime

class ContentExtractor(HTMLParser):
    """Extract main content and titles from HTML pages."""
    def __init__(self):
        super().__init__()
        self.in_main = False
        self.in_title = False
        self.in_nav = False
        self.in_footer = False
        self.main_content = []
        self.title = ""
        self.h1_title = ""
        self.depth = 0
        
    def handle_starttag(self, tag, attrs):
        attrs_dict = dict(attrs)
        
        # Skip navigation and footer
        if tag in ['nav', 'footer'] or attrs_dict.get('class', '').find('nav') >= 0:
            self.in_nav = True
            return
        if attrs_dict.get('class', '').find('footer') >= 0:
            self.in_footer = True
            return
            
        # Track main content
        if tag == 'main' or attrs_dict.get('data-pagefind-body') is not None:
            self.in_main = True
            self.depth = 0
            
        if tag == 'title':
            self.in_title = True
            
        # Collect content from main section
        if self.in_main and not self.in_nav and not self.in_footer:
            # Track h1 for title
            if tag == 'h1' and not self.h1_title:
                pass  # We'll grab the text in handle_data
            
            # Reconstruct tag with attributes
            attr_str = ''
            for key, value in attrs:
                attr_str += f' {key}="{value}"'
            self.main_content.append(f'<{tag}{attr_str}>')
            self.depth += 1
    
    def handle_endtag(self, tag):
        if tag in ['nav'] and self.in_nav:
            self.in_nav = False
            return
        if tag == 'footer' and self.in_footer:
            self.in_footer = False
            return
            
        if tag == 'title':
            self.in_title = False
            
        if self.in_main and not self.in_nav and not self.in_footer:
            self.main_content.append(f'</{tag}>')
            self.depth -= 1
            if self.depth == 0:
                self.in_main = False
    
    def handle_data(self, data):
        if self.in_title:
            self.title = data.strip()
        elif self.in_main and not self.in_nav and not self.in_footer:
            self.main_content.append(data)
    
    def handle_startendtag(self, tag, attrs):
        if self.in_main and not self.in_nav and not self.in_footer:
            attr_str = ''
            for key, value in attrs:
                attr_str += f' {key}="{value}"'
            self.main_content.append(f'<{tag}{attr_str} />')
    
    def get_content(self):
        return ''.join(self.main_content)

def extract_content_from_html(file_path):
    """Extract main content and title from an HTML file."""
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            html = f.read()
        
        parser = ContentExtractor()
        parser.feed(html)
        
        return {
            'title': parser.title,
            'content': parser.get_content()
        }
    except Exception as e:
        print(f"Warning: Could not process {file_path}: {e}", file=sys.stderr)
        return None

def collect_html_files(site_dir):
    """Collect all HTML files from the site directory."""
    html_files = []
    
    # Define the order of collections/sections
    ordered_sections = [
        'index.html',
        'introduction.html',
        '_chapters',
        '_instructions',
        '_macros',
        '_commands',
        '_heads',
        '_tools',
        '_plugins',
    ]
    
    site_path = Path(site_dir)
    
    # Collect files in order
    files_found = []
    
    for section in ordered_sections:
        if section.endswith('.html'):
            # Direct file
            file_path = site_path / section
            if file_path.exists():
                files_found.append(str(file_path))
        else:
            # Directory
            dir_path = site_path / section
            if dir_path.exists():
                # Sort files within each directory
                section_files = sorted(dir_path.glob('*.html'))
                files_found.extend([str(f) for f in section_files])
    
    return files_found

def create_single_html(site_dir, output_file, version):
    """Create a single HTML file from all documentation pages."""
    
    html_files = collect_html_files(site_dir)
    
    if not html_files:
        print(f"Error: No HTML files found in {site_dir}", file=sys.stderr)
        return False
    
    print(f"Found {len(html_files)} HTML files to process...")
    
    # Extract content from all files
    sections = []
    for html_file in html_files:
        result = extract_content_from_html(html_file)
        if result and result['content'].strip():
            rel_path = os.path.relpath(html_file, site_dir)
            sections.append({
                'file': rel_path,
                'title': result['title'] or rel_path,
                'content': result['content']
            })
    
    print(f"Successfully processed {len(sections)} pages...")
    
    # Generate the combined HTML
    generation_date = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    html_output = f'''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bnd Documentation - {version}</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            line-height: 1.6;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            color: #333;
        }}
        
        .header {{
            border-bottom: 3px solid #0066cc;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }}
        
        .header h1 {{
            color: #0066cc;
            margin-bottom: 5px;
        }}
        
        .header .meta {{
            color: #666;
            font-size: 14px;
        }}
        
        .toc {{
            background: #f5f5f5;
            padding: 20px;
            margin-bottom: 30px;
            border-left: 4px solid #0066cc;
        }}
        
        .toc h2 {{
            margin-top: 0;
            color: #0066cc;
        }}
        
        .toc ul {{
            list-style-type: none;
            padding-left: 0;
        }}
        
        .toc li {{
            margin: 8px 0;
        }}
        
        .toc a {{
            color: #0066cc;
            text-decoration: none;
        }}
        
        .toc a:hover {{
            text-decoration: underline;
        }}
        
        .section {{
            margin-bottom: 50px;
            page-break-inside: avoid;
        }}
        
        .section-header {{
            border-bottom: 2px solid #e0e0e0;
            padding-bottom: 10px;
            margin-bottom: 20px;
        }}
        
        .section-header h2 {{
            color: #0066cc;
            margin: 0;
        }}
        
        .section-header .source {{
            color: #999;
            font-size: 12px;
            font-family: monospace;
        }}
        
        pre {{
            background: #f5f5f5;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
        }}
        
        code {{
            background: #f0f0f0;
            padding: 2px 5px;
            border-radius: 3px;
            font-family: 'Courier New', Courier, monospace;
        }}
        
        pre code {{
            background: none;
            padding: 0;
        }}
        
        table {{
            border-collapse: collapse;
            width: 100%;
            margin: 20px 0;
        }}
        
        th, td {{
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }}
        
        th {{
            background-color: #f5f5f5;
            font-weight: bold;
        }}
        
        @media print {{
            body {{
                max-width: none;
                padding: 0;
            }}
            
            .toc {{
                page-break-after: always;
            }}
            
            .section {{
                page-break-inside: avoid;
            }}
            
            a {{
                color: #000;
                text-decoration: none;
            }}
        }}
    </style>
</head>
<body>
    <div class="header">
        <h1>Bnd Documentation</h1>
        <div class="meta">
            Version: {version}<br>
            Generated: {generation_date}<br>
            Source: <a href="https://bnd.bndtools.org">https://bnd.bndtools.org</a>
        </div>
    </div>
    
    <div class="toc">
        <h2>Table of Contents</h2>
        <ul>
'''
    
    # Add TOC entries
    for i, section in enumerate(sections):
        section_id = f"section-{i}"
        html_output += f'            <li><a href="#{section_id}">{section["title"]}</a></li>\n'
    
    html_output += '''        </ul>
    </div>
    
'''
    
    # Add all sections
    for i, section in enumerate(sections):
        section_id = f"section-{i}"
        html_output += f'''    <div class="section" id="{section_id}">
        <div class="section-header">
            <h2>{section["title"]}</h2>
            <div class="source">Source: {section["file"]}</div>
        </div>
        <div class="section-content">
{section["content"]}
        </div>
    </div>
    
'''
    
    html_output += '''</body>
</html>
'''
    
    # Write output file
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(html_output)
    
    print(f"✓ Successfully created {output_file}")
    print(f"  - Total sections: {len(sections)}")
    print(f"  - File size: {len(html_output) / 1024:.1f} KB")
    
    return True

# Run the script
if __name__ == '__main__':
    import sys
    site_dir = os.environ.get('SITE_DIR', '_site')
    output_file = os.environ.get('OUTPUT_FILE', 'bnd-docs.html')
    version = os.environ.get('VERSION', 'master')
    
    success = create_single_html(site_dir, output_file, version)
    sys.exit(0 if success else 1)

PYTHON_SCRIPT

echo ""
echo "Export complete!"
echo "Output file: ${OUTPUT_FILE}"
echo ""
