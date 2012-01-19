---
title: Release Notes
description: Notes for the latest Bndtools releases
author: Neil Bartlett
---

Version 1.0.0
=============

See [what's new in Bndtools version 1.0.0](/whatsnew1-0-0/).

Version 1.0.1 (NOT RELEASED)
===========================

### Resolution paths in Run Bundles ###

Previously the generated list of Run Bundles resulting from OBR resolution contained a `resolution` attribute giving the absolute URL of the resolved resource. These paths are not used by Bndtools but they may be used by other tools (e.g. ANT tasks) to generate packaging artefacts based on the resolution results. However users reported that the presence of this generated information in a source file led to problems with sharing the file through version control (see [bug 315](https://github.com/bndtools/bndtools/issues/315)).

In this release, resolution paths are no longer generated directly in the bnd or bndrun file. Instead they are output into a file under the `generated` directory, named `<runfile>.resolved`. The format for the information is the same, i.e.:

	<bsn>;version=<version>;resolution=<url>

