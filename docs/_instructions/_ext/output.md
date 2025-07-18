---
layout: default
class: Analyzer
title: -output  FILE
summary: Specify the output directory or file.
---

The `-output` instruction allows you to specify the output directory or file for the generated JAR or bundle. You can provide a file path or directory; if a directory is specified, the output file will be named based on the bundle symbolic name and version (e.g., `bsn-version.jar`).

If no output is specified, bnd will use default naming strategies, such as the name of the source file or a generic name like `Untitled`. This instruction is useful for controlling where and how your build artifacts are saved.

Note: See also the `-outputmask` instruction for more advanced output naming options.

<hr />
TODO Needs review - AI Generated content