# Release Cycle

The following release cycle is used:

```
----> DEV ---> RCx ---> REL --+
 ^ ^       ^        |         |
 | |       |        |         |
 | |       +-- +1 --+         |
 | |        next RC |         |
 | |                |         |
 | +----------------+         |
 |  cancelled release         |
 |                            |
 +----------------------------+
  next release cycle

  with x e {1, 2, 3, ...}
```

The versions to use:
* DEV: `a.b.c.DEV`
* RCx: `a.b.c.RCx`
* REL: `a.b.c.REL`


# Prerequisites

You must:
* Be a bndtools committer
* Have checked out the source code in a directory of your choice
* Not be afraid to use the command-line
* Have `Git` installed in such a fashion that the `git` executable is in the search path.

# Release Procedure for Bndtools

The procedure described here is going to release version `a.b.c.REL` from the `master`
branch in a local checkout of the source code. You can obviously release from a different
branch, this is just an example.

We assume that changes to the source code will be pushed to the Git remote repository with
the `origin` alias.


* Make sure that the `Changes-in-a-b-c` page is added to / present on the
[changelogs page](https://github.com/bndtools/bndtools/wiki/Changelogs). If it is not
present, then edit the page, add the snippet below, and save the page.

```
[Changes in a.b.c](https://github.com/bndtools/bndtools/wiki/Changes-in-a.b.c)
```

* Click on the `Changes in a.b.c` page link to go to it. Usually the `Changes-in-a-b-c`
page will be build up during development. Make sure it is up-to-date.

* Open a shell and go to the directory in which you have checked out the source code.
* Switch to the `master` branch and make sure you have a clean checkout
(this will destroy any changes)

```
git checkout master
git clean -fdx
git reset --hard
```

* Update the master version in the source code. Adjust the version settings in the
file `cnf/build.bnd`:

```
base-version:             a.b.c
base-version-qualifier:   REL
```

* Commit, tag and push the version change.

```
git add cnf/build.bnd
git commit -s -m "Update version to a.b.c.REL"
git tag -s -m "a.b.c.REL" a.b.c.REL
git push origin master a.b.c.REL
```

* Let Jenkins on [Cloudbees][2] build the release, wait
for the build to finish and be successful.
* Lock the Cloudbees build so that it is kept forever, and set the build information to
`Bndtools a.b.c.REL`
* Download the ZIP file [bndtools-latest.zip][5] from Cloudbees.
* Create a new a.b.c version in [Bintray][1].
* Unpack the ZIP file and store the contents in the a.b.c version at [Bintray][1].
* Also update the latest version at [Bintray][1] with the content of the ZIP file.
* Add the `org.bndtools.templates.*` bundles from the [release repo][4] on Cloudbees
to [Bundle-hub][3].

* Update the versions for the next development cycle in the file `cnf/build.bnd`:

```
baseline.version:         a.b.c
base-version:             a.d.0
base-version-qualifier:   DEV
```

* Commit, tag and push the version change.

```
git add cnf/build.bnd
git commit -s -m "Update version to a.d.0.DEV"
git tag -s -m "a.d.0.DEV" a.d.0.DEV
git push origin master a.d.0.DEV
```

[1]: https://bintray.com/bndtools/bndtools/update/view
[2]: https://bndtools.ci.cloudbees.com/
[3]: https://github.com/bndtools/bundle-hub
[4]: https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/build/generated/repo/
[5]: https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/build/generated/bndtools-latest.zip
