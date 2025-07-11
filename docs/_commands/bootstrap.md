---
layout: default
title: bootstrap
summary: |
   Interactive gogo shell
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   bootstrap  ...


<!-- Manual content from: ext/bootstrap.md --><br /><br />
## Examples

	/**
	 * start a local framework
	 */
	
	interface BootstrapOptions extends Options {
		
	}
	
	public void _bootstrap(BootstrapOptions options) throws Exception {
		Workspace ws = getWorkspace(getBase());
		File buildDir = ws.getBuildDir();
		File bndFile = IO.getFile(buildDir, "bnd.bnd");
		if ( !bndFile.isFile()) {
			error("No bnd.bnd file found in cnf directory %s", bndFile);
			return;
		}
		
		Run run = new Run(ws, buildDir, bndFile);
		
		run.runLocal();
		
		getInfo(run);
	}
