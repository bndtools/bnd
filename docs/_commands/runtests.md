---
layout: default
title:   runtests [options] ... 
summary: Run OSGi tests and create report 
---

## Description

{{page.summary}}

## Synopsis

runtests [options] ...

## Options

    [ -d, --dir <string> ]     - Path to work directory
    [ -e, --exclude <string;> ] - Exclude files by pattern
    [ -r, --reportdir <string> ] - Report directory
    [ -t, --tests <string;> ]  - Test names to execute
    [ -T, --title <string> ]   - Title in the report
    [ -v, --verbose ]          - prints more processing information
    [ -w, --workspace <string> ] - Use the following workspace

## Examples

    bnd runtests --tests org.osgi.test.cases.tracker.junit.BundleTrackerTests:testSubclass,org.osgi.test.cases.tracker.junit.BundleTrackerTests:testModified org.osgi.test.cases.tracker.bnd
