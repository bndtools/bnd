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

The procedure described here is going to release version `a.b.c.REL` from the `master` branch in a local checkout of the source code. You can obviously release from a different branch, this is just an example.

We assume that changes to the source code will be pushed to the Git remote repository with the `github` alias.


* Make sure that the `Changes-in-a-b-c` page is added to / present on the [changelogs page](https://github.com/bndtools/bndtools/wiki/Changelogs). If it is not present, then edit the page, add the snippet below, and save the page.

```
[Changes in a.b.c](https://github.com/bndtools/bndtools/wiki/Changes-in-a.b.c)
```

* Click on the `Changes in a.b.c` page link to go to it. Usually the `Changes-in-a-b-c` page will be build up during development as the `Changes-on-master` page. If that page is present, then copy its contents into the `Changes-in-a-b-c` page and adjust it. Otherwise, just make sure it is up-to-date.
* Open a bash shell and enter the directory in which you have checked out the source code.
* Clean the source code checkout (this will destroy any changes)

```
git clean -fdx
git reset --hard
```

* Switch to the `master` branch

```
git checkout master
```

* Update the master version in the source code. Adjust the version settings in the file ```cnf/build.bnd```:

```
base-version:             a.b.c
base-version-qualifier:   REL
```

* Check in the changes into Git

```
git add cnf/build.bnd
git commit -s -m "Update version to a.b.c.REL"
```

* Create a Git tag and push it (and the `master` branch as well)

```
git tag -a -m "Tag a.b.c.REL" a.b.c.REL
git push github master a.b.c.REL
```

* Let Jenkins on [Cloudbees](https://bndtools.ci.cloudbees.com/) build the release, wait for the build to finish and be successful.
* Lock the Cloudbees build so that it is kept forever, and set the build information to `Bndtools a.b.c.REL`
* Download a ZIP file with the relevant archived artifacts from https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/build/generated/*zip*/generated.zip
* Unpack the ZIP file and store the following artifacts in the `releases` repository (https://github.com/bndtools/releases). These artifacts must be stored in the `bndtools/a.b.c.REL` directory of the `releases` repository.
  * generated/extras
  * generated/p2
  * generated/bndtools-extras-latest.zip
  * generated/bndtools-latest.zip
* Push the changes in the `release` repository.
* Update the versions for the next development build, adjust the version settings in the file ```cnf/build.bnd```:

```
base-version:             a.d.0
base-version-qualifier:   DEV
```

* Commit and push the version change.

```
git add cnf/build.bnd
git commit -s -m "Update version to a.d.0.DEV"
git push github master
```
