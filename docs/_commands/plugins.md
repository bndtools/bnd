---
layout: default
title: plugins
summary: |
   Show the loaded workspace plugins
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   plugins [options]  ...


#### Options: #
- `[ -p --project <string> ]` Identify another project

<!-- Manual content from: ext/plugins.md --><br /><br />

## Examples

	biz.aQute.bnd (master)$ bnd plugins
	found password 
	000 aQute.bnd.build.Workspace
	001 java.util.concurrent.ThreadPoolExecutor@2685c106[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0]
	002 java.util.Random@174384ac
	003 aQute.bnd.maven.support.Maven@51bb4422
	004 aQute.lib.settings.Settings@5d763e19
	005 bnd-cache
	006 aQute.bnd.resource.repository.ResourceRepositoryImpl@67528259
	007 aQute.bnd.osgi.Processor
	008 /Ws/bnd/cnf/repo                         r/w=true
	009 /Ws/bnd/dist/bundles                     r/w=true
	010 aQute.bnd.signing.JartoolSigner@2dac2cb7
