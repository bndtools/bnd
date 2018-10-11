---
layout: default
title:    deliverables [options]
summary:  Show all deliverables from this workspace. with their current version and path.
---

## Description

{{page.summary}}

## Synopsis

## Options

	[ -l, --limit ]            - Only provide deliverables of this project
	[ -p, --project <string> ] - Path to project, default current directory

## Example 

	biz.aQute.bnd (master)$ bnd deliverables 
	found password 
	aQute.libg                                  2.9.0  /Ws/bnd/aQute.libg/generated/aQute.libg.jar
	biz.aQute.bnd                               2.4.0  /Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar
	biz.aQute.bnd.annotation                    2.4.0  /Ws/bnd/biz.aQute.bnd.annotation/generated/biz.aQute.bnd.annotation.jar
	biz.aQute.bnd.bootstrap.console             0.0.0  /Ws/bnd/biz.aQute.bnd.bootstrap/generated/biz.aQute.bnd.bootstrap.console.jar
	biz.aQute.bnd.test                          2.4.0  /Ws/bnd/biz.aQute.bnd.test/generated/biz.aQute.bnd.test.jar
	biz.aQute.bnd.testextension                 2.4.0  /Ws/bnd/biz.aQute.bnd.testextension/generated/biz.aQute.bnd.testextension.jar
	biz.aQute.bndlib                            2.4.0  /Ws/bnd/biz.aQute.bndlib/generated/biz.aQute.bndlib.jar
	biz.aQute.bndlib.tests                      2.4.0  /Ws/bnd/biz.aQute.bndlib.tests/generated/biz.aQute.bndlib.tests.jar
	biz.aQute.junit                             1.3.0  /Ws/bnd/biz.aQute.junit/generated/biz.aQute.junit.jar
	biz.aQute.launcher                          1.4.0  /Ws/bnd/biz.aQute.launcher/generated/biz.aQute.launcher.jar
	biz.aQute.repository                        2.2.0  /Ws/bnd/biz.aQute.repository/generated/biz.aQute.repository.jar
	biz.aQute.resolve                           0.2.0  /Ws/bnd/biz.aQute.resolve/generated/biz.aQute.resolve.jar
	cnf                                         0.0.0  /Ws/bnd/cnf/generated/cnf.jar
	demo                                        1.1.0  /Ws/bnd/demo/generated/demo.jar
	dist                                        0.0.0  /Ws/bnd/dist/generated/dist.jar
	osgi.r5                                     1.0.1  /Ws/bnd/osgi.r5/generated/osgi.r5.jar
	biz.aQute.bnd (master)$ bnd deliverables -l
	found password 
	biz.aQute.bnd                               2.4.0  /Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar
	biz.aQute.bnd (master)$ 
