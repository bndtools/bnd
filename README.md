# Bndtools: OSGi Development Tools for Eclipse
=====================

Please visit the [Bndtools Home Page](http://bndtools.org) for full documentation.

## Installation
Install using the Eclipse installer, using the following update site URL:

```
https://dl.bintray.com/bndtools/bndtools/latest/
```

If you are feeling brave, install the latest alpha version from the following update site URLs instead:

```
https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/build/generated/p2/
```

## License
BndTools is licensed under the [Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html).

## Building
Gradle is used to build bndtools. The workspace root has a `build.gradle` file that builds all projects in proper order.

`./gradlew`              - Assembles and packages the projects into build/generated  

The workspace root also includes the gradle wrapper, `gradlew`, command.

[![CloudBees Build Status](https://bndtools.ci.cloudbees.com/job/bndtools.master/badge/icon)](https://bndtools.ci.cloudbees.com/job/bndtools.master/)
[![Travis CI Build Status](https://travis-ci.org/bndtools/bndtools.svg?branch=master)](https://travis-ci.org/bndtools/bndtools)

## Release
bndtools is continuously built and released on [CloudBees](https://bndtools.ci.cloudbees.com/).

[![Built on DEV@cloud](http://www.cloudbees.com/sites/default/files/Button-Built-on-CB-1.png)](http://www.cloudbees.com/foss/foss-dev.cb)

## Feedback
Feedback is always welcome, for general discussions use the <bndtools-users@googlegroups.com> list. We also have the <bndtools-dev@googlegroups.com> list for discussions on the development of bndtools.

Bugs and issues should go to <https://github.com/bndtools/bndtools/issues>

## Contributing

Want to hack on bndtools? There are [instructions](CONTRIBUTING.md) to get you
started.

They are probably not perfect, please let us know if anything feels
wrong or incomplete.
