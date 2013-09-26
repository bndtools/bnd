Release Cycle
=============

The following release cycle is used:

	----> DEV ---> RCxx ---> REL --+
	 ^ ^       ^         |         |
	 | |       |         |         |
	 | |       +-- +1 ---+         |
	 | |        next RC  |         |
	 | |                 |         |
	 | +-----------------+         |
	 |  cancelled release          |
	 |                             |
	 +-----------------------------+
	  next release cycle

	  with xx e [01, 99]


The versions to use:
* DEV: a.b.c.DEV-qualifier
* RCxx: a.b.c.RCxx-qualifier
* REL: a.b.c.REL


Release Checklist for Bndtools
==============================

Update Whats New
----------------

Create a whatsnewA-B-C.html page in the bndtools.github.com repo.

Update Versions
---------------

1. Update `base-version` in `cnf/build.bnd`.
2. Update versions in
    * `build/feature/extras/ace/feature.xml`
    * `build/feature/extras/amdatu/feature.xml`
    * `build/feature/extras/category.xml`
    * `build/feature/extras/dm/feature.xml`
    * `build/feature/main/bndtools/feature.xml`
    * `build/feature/main/category.xml`
    * `build/feature/main/jarviewer/feature.xml`
    * `bndtools.core/resources/intro/whatsnewExtensionContent.xml`

Git Tag
-------

	git tag -a <version>
	git push

Build
-----

	git clean -fdx     (Note: will destroy any unversioned changes)
	git reset --hard   (Note: will destroy any uncommitted changes)
	ant p2


IMPORTANT NOTES
---------------
* We should release bnd and bndtools at the same time, with the same version.
* The builds corresponding to the releases should be kept forever on the cloudbees Jenkins server
