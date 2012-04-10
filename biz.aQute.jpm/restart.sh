#!/bin/sh -e
java -jar generated/biz.aQute.jpm.run.jar -et deinit -f
java -jar generated/biz.aQute.jpm.run.jar -et init
jpm -et install -f generated/biz.aQute.jpm.daemon.jar 
jpm -et start testd

