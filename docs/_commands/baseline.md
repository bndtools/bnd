---
layout: default
title: baseline [options] <[newer jar]> <[older jar]> 
summary: Compare a newer bundle to a baselined bundle and provide versioning advice.                                
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -a, --all ]              - Show all, also unchanged
    [ -d, --diff ]             - Show any differences
    [ -f, --fixup <string> ]   - Output file with fixup info
    [ -p, --packages]          - Packages to baseline (comma delimited)
    [ -q, --quiet ]            - Be quiet, only report errors
    [ -v, --verbose ]          - On changed, list API changes

## Examples
`bnd baseline --diff newer.jar older.jar`

