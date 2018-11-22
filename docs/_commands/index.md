---
layout: default
title: index [options] <[bundles]...>
summary: Index bundles from the local file system
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -b, --base <uri> ]       - URI from which to make paths in the index file
                                relative (default: relative to the output file
                                directory). The specified value must be a prefix
                                of the absolute output file directory in order
                                to have any effect
    [ -d, --directory <file> ] - The directory to write the repository index file
                                (default: the current directory)
    [ -n, --name <string> ]    - The name of the index (default: name of the
                                output file directory)
    [ -r, --repositoryIndex <file> ] - The name of the repository index file
                                (default: 'index.xml'). To enable GZIP
                                compression use the file extension '.gz' (e.g.
                                'index.xml.gz')

## Examples
