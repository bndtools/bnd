---
layout: default
title: bootstrap 
summary: Interactive gogo shell                                 
---

## Description

{{page.summary}}

## Synopsis

## Options

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
