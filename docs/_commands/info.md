---
layout: default
title:  info [options] 
summary: Show key project variables
---

## Description

{{page.summary}}

## Synopsis

## Options

	[ -b, --buildpath ]        - 
	[ -c, --classpath ]        - 
	[ -d, --dependsOn ]        - 
	[ -p, --project <string> ] - 
	[ -r, --runbundles ]       - 
	[ -s, --sourcepath ]       - 
	[ -v, --vmpath ]           - 

## Examples

	biz.aQute.bnd (master)$ bnd info -b
	found password 
	Build                                    [/Ws/bnd/biz.aQute.bnd/bin, /Ws/bnd/aQute.libg/generated/aQute.libg.jar, /Ws/bnd/biz.aQute.bndlib/bin, /Ws/bnd/cnf/repo/org.apache.tools.ant/org.apache.tools.ant-1.6.5.jar, /Ws/bnd/cnf/repo/org.osgi.service.component.annotations/org.osgi.service.component.annotations-6.0.0.jar, /Ws/bnd/cnf/repo/osgi.cmpn/osgi.cmpn-4.3.1.jar, /Ws/bnd/cnf/repo/osgi.core/osgi.core-4.3.1.jar, /Ws/bnd/cnf/repo/org.osgi.impl.bundle.bindex/org.osgi.impl.bundle.bindex-2.2.0.jar, /Ws/bnd/cnf/repo/osgi.r5/osgi.r5-1.0.1.jar]

	Class path                               []

	Depends on                               [aQute.libg, biz.aQute.bndlib, biz.aQute.junit, biz.aQute.launcher]

	Run                                      []

	Run path                                 [/Ws/bnd/cnf/repo/org.eclipse.osgi/org.eclipse.osgi-3.6.0.jar, /Ws/bnd/cnf/repo/com.springsource.junit/com.springsource.junit-3.8.2.jar]

	Source                                   [/Ws/bnd/biz.aQute.bnd/src]

	biz.aQute.bnd (master)$ bnd info -bcdrsv
	found password 
	Build                                    [/Ws/bnd/biz.aQute.bnd/bin, /Ws/bnd/aQute.libg/generated/aQute.libg.jar, /Ws/bnd/biz.aQute.bndlib/bin, /Ws/bnd/cnf/repo/org.apache.tools.ant/org.apache.tools.ant-1.6.5.jar, /Ws/bnd/cnf/repo/org.osgi.service.component.annotations/org.osgi.service.component.annotations-6.0.0.jar, /Ws/bnd/cnf/repo/osgi.cmpn/osgi.cmpn-4.3.1.jar, /Ws/bnd/cnf/repo/osgi.core/osgi.core-4.3.1.jar, /Ws/bnd/cnf/repo/org.osgi.impl.bundle.bindex/org.osgi.impl.bundle.bindex-2.2.0.jar, /Ws/bnd/cnf/repo/osgi.r5/osgi.r5-1.0.1.jar]

	Class path                               []

	Depends on                               [aQute.libg, biz.aQute.bndlib, biz.aQute.junit, biz.aQute.launcher]

	Run                                      []

	Run path                                 [/Ws/bnd/cnf/repo/org.eclipse.osgi/org.eclipse.osgi-3.6.0.jar, /Ws/bnd/cnf/repo/com.springsource.junit/com.springsource.junit-3.8.2.jar]

	Source                                   [/Ws/bnd/biz.aQute.bnd/src]
