Release Checklist for Bndtools
==============================

Update Versions
---------------

1.	Update `base-version` in `cnf/build.bnd`.
2.	Update versionss in `bndtools.build/feature/main/feature.xml` AND `../jarviewer/feature.xml`.
3.	Update version in `bndtools.build/feature/category.xml`

Git Tag
-------

	git tag -a <version>
	git push

Build
-----

	cd bndtools.build
	rm -r generated
	ant clean p2
