---
layout: default
title:   digest [options] <[file...]> 
summary: Digest a number of files 
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -a, --algorithm <alg>* ] - Specify the algorithms
    [ -b, --b64 ]              - Show base64 output
    [ -h, --hex ]              - Show hex output (default)
    [ -n, --name ]             - Show the file name
    [ -p, --process ]          - Show process info


## Examples
    biz.aQute.bnd (master)$ bnd digest generated/biz.aQute.bnd.jar 
    16B415286B53FA499BD7B2684A93924CA7C198C8
