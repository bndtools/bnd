---
layout: default
title:   repo [options] <[sub-cmd]> ...  
summary: Access to the repositories. Provides a number of sub commands to manipulate the repository (see repo help) that provide access to the installed repos for the current project.
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -c, --cache ]            - Include the cache repository
    [ -f, --filerepo <string>* ] - Add a File Repository
    [ -m, --maven ]            - Include the maven repository
    [ -p, --project <string> ] - Specify a project
    [ -r, --release <glob> ]   - Override the name of the release repository
                                (-releaserepo)
    Available sub-commands: 

      copy                        -  
      diff                        - Diff jars (or show tree) 
      get                         - Get an artifact from a repository. 
      list                        - List all artifacts from the current repositories
                                    with their versions 
      put                         - Put an artifact into the repository after it has
                                    been verified. 
      refresh                     - Refresh refreshable repositories 
      repos                       - List the current repositories 
      versions                    - Displays a list of versions for a given bsn that
                                    can be found in the current repositories. 

## Examples

## Sub-commands

### Copy

#### Description

#### Synopsis

    copy [options] <source> <dest>

#### Options

   [ -d, --dry ]              - Do not really copy but trace the steps

### diff

#### Description

Show the diff tree of a single repo or compare 2
repos. A diff tree is a detailed tree of all
aspects of a bundle, including its packages,
types, methods, fields, and modifiers.


#### Synopsis

    diff [options] <newer repo> <[older repo]>

#### Options

    [ -a, --added ]            - Just additions (no removes)
    [ -A, --all ]              - Both add and removes
    [ -d, --diff ]             - Formatted like diff
    [ -f, --full ]             - Show full diff tree (also wen entries are equal)
    [ -j, --json ]             - Serialize to JSON
    [ -r, --remove ]           - Just removes (no additions)

### get

#### Description

Get an artifact from a repository.

#### Synopsis

    get [options] <bsn> <[range]>

#### Options

    [ -f, --from <instruction> ] - 
    [ -l, --lowest ]           - 
    [ -o, --output <string> ]  - Where to store the artifact


### list

#### Description

List all artifacts from the current repositories with their versions

#### Synopsis

    list [options] 

#### Options

    [ -f, --from <instruction> ] - A glob expression on the source repo, default
                                is all repos
    [ -n, --noversions ]       - Do not list the versions, just the bsns
    [ -q, --query <string> ]   - Optional search term for the list of bsns (given
                                to the repo)

### put

#### Description

Put an artifact into the repository after it has been verified.

#### Synopsis

    put [options] <<jar>...>

#### Options

    [ -f, --force ]            - Put in repository even if verification fails
                                (actually, no verification is done).

### refresh

#### Description

Refresh refreshable repositories

#### Synopsis

    refresh

#### Options

### repos

#### Description

List the current repositories

#### Synopsis

    repos

#### Options

### versions

#### Description

Displays a sorted set of versions for a given bsn that can be found in the current repositories.

#### Synopsis

    versions <bsn>

#### Options
