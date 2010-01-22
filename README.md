Bnd Tools for Eclipse
=====================

This is an Eclipse feature containing some simple tools for developing OSGi bundles with [bnd](http://www.aqute.biz/Code/Bnd). It offers the following features:

1. A rich multipage editor for editing Bnd descriptor files. Access to advanced Bnd features is always available through the "source" tab.

2. Continuous incremental builds of bundles when either the Bnd file or one of the classes contained in the bundle is changed.

3. Easy launching of an OSGi runtime (Equinox and Felix supported so far, Knopflerfish and Concierge soon).

4. Examine dependencies of a selected bundle (either Bnd source or a bundle JAR) in the Imports/Exports view.

5. Refine the dependencies of a bundle by dragging packages from the Imports/Exports view into the editor's imports list, then set them all to optional or tweak the version ranges.

6. Easily declare DS components by dragging a component class into the Components list in the editor.

Installation
------------

Install using the Eclipse installer, using the following update site URL:

	http://bndtools-updates.s3.amazonaws.com/

Licence
-------

BndTools is licensed under the [Eclipse Public Licence v1.0](http://www.eclipse.org/legal/epl-v10.html).

Known Bugs and Limitations
--------------------------

1. When an OSGi specification level (e.g. R4.2) is selected rather than a specific framework installation, the physical framework used is arbitrary. The UI for mapping specification levels to preferred implementations has not yet been written.

2. 