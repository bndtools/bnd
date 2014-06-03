# Bndtools: OSGi Development Tools for Eclipse
=====================

Please visit the [Bndtools Home Page](http://bndtools.org) for full documentation.

## Installation
Install using the Eclipse installer, using the following update site URL:

```
http://bndtools-updates.s3.amazonaws.com/
```

If you are feeling brave, install the latest alpha version from the following update site URLs instead:

```
https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/build/generated/p2/
https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/build/generated/extras/p2/
```

## License
BndTools is licensed under the [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html).

## Building
Gradle is used to build bndtools. The workspace root has a `build.gradle` file that builds all projects in proper order.

`gradle`              - Assembles and packages the projects into build/generated  

The workspace root also includes the gradle wrapper, `gradlew`, command if you do not have gradle installed
on your system.

[![CloudBees Build Status](https://bndtools.ci.cloudbees.com/job/bndtools.master/badge/icon)](https://bndtools.ci.cloudbees.com/job/bndtools.master/)
[![Travis CI Build Status](https://travis-ci.org/bndtools/bndtools.svg?branch=master)](https://travis-ci.org/bndtools/bndtools)

## Release
bndtools is continuously built and released on [CloudBees](https://bndtools.ci.cloudbees.com/).

[![Built on DEV@cloud](http://www.cloudbees.com/sites/default/files/Button-Built-on-CB-1.png)](http://www.cloudbees.com/foss/foss-dev.cb)
