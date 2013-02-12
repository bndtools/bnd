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

Update Versions
---------------

1. Update `base-version` in `cnf/build.bnd`.
2. Update versions in
    * `bndtools.build/feature/ace/feature.xml`
    * `bndtools.build/feature/jarviewer/feature.xml`
    * `bndtools.build/feature/main/feature.xml`
    * `bndtools.build/feature/category.xml`

Git Tag
-------

	git tag -a <version>
	git push

Build
-----

	git clean -fdx     (Note: will destroy any unversioned changes)
	git reset --hard   (Note: will destroy any uncommitted changes)
	ant p2
