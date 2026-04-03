# Patch Release Documentation - Wiki Integration Instructions

This document provides instructions for adding the patch release documentation to the GitHub wiki.

## File Location

The patch release documentation has been created in this repository at:
`docs/patch-release-documentation.md`

## Integration into Wiki

The content from `docs/patch-release-documentation.md` should be added to the GitHub wiki at:
https://github.com/bndtools/bnd/wiki/Release-Process

### Recommended Integration Approach

Add a new section to the Release Process wiki page titled:

**"Patch Release Process (MICRO Version Increase)"**

This section should be inserted after the "RC2..." section and before the "Release" section, or as a standalone section at the end of the page with a link in the table of contents.

### Alternative Approach

Create a separate wiki page:
- Page name: "Patch-Release-Process"
- Link from the main Release Process page

### Content Highlights

The patch release documentation covers:

1. **When to Use Patch Releases** - Guidelines for when a patch release is appropriate
2. **Key Differences from Regular Releases** - Important distinctions including:
   - Master branch is NOT updated
   - RC1 builds with previous release (not RC range)
   - RC2+ builds with RC range
   - About.java gets patch version constant with 3 components
3. **Preparation for First Patch Release Candidate** - Complete step-by-step instructions
4. **Subsequent Patch Release Candidates** - Instructions for RC2, RC3, etc.
5. **Final Patch Release** - Reference to existing release documentation
6. **Complete Workflow Example** - A full example workflow from start to finish

### Wiki Update Instructions

To add this to the wiki:

1. Go to https://github.com/bndtools/bnd/wiki/Release-Process/_edit
2. Choose where to insert the patch release section (recommended: near the end, after RC2 section)
3. Copy the content from `docs/patch-release-documentation.md`
4. Paste it into the wiki editor
5. Adjust any formatting or links as needed
6. Save the page

Alternatively, create a new wiki page:
1. Go to https://github.com/bndtools/bnd/wiki/_new
2. Title: "Patch-Release-Process"
3. Paste content from `docs/patch-release-documentation.md`
4. Save
5. Add a link to this page from the main Release Process page

## Testing the Documentation

The documentation has been tested with dry-run mode for the following scenarios:
- patch-first-rc for 7.2.1-RC1 (builds with [7.2.0,7.3.0))
- patch-next-rc for 7.2.1-RC2 (builds with [7.2.1-RC,7.3.0))
- release for 7.2.1 (final release)
- Validation that patch modes reject MICRO version = 0

All tests passed successfully.
