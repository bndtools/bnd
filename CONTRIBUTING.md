# Contributing to Bnd/Bndtools

Want to hack on Bnd/Bndtools? Here are instructions to get you started. They are probably not perfect, please let us know if anything feels wrong or incomplete.

## Reporting Issues

When reporting [issues](https://github.com/bndtools/bnd/issues) on GitHub please include the version of Bnd/Bndtools you are using, `bnd version`, as well as the version of Java, `java -version`, and your OS.
Also, please provide the `Git-SHA` and `Git-Descriptor` headers from the Bnd/Bndtools jar file.
Please include the steps required to reproduce the problem if possible and applicable.
This information will help us review and fix your issue faster.


## Development Setups

We provide pre-configured setups using Eclipse oomph installer, which help you getting started quickly. 
This way you get a dedicated Eclipse instance with pre-installed bndtools source code with a simple one-click installer.

**Find our different setups and P2 Repositories at:**
<https://bndtools.org/bndtools.p2.repo/>

## Launching Bndtools from Eclipse

To launch bndtools from Eclipse (e.g. to try out a change to debug), use one of the `.bndrun` files from the `bndtools.core` project. There are three launchers, one per architecture, i.e.:

* `bndtools.cocoa.macosx.x86_64.bndrun` for running on MacOS (64-bit Intel x86)
* `bndtools.cocoa.macosx.aarch64.bndrun` for running on MacOS (64-bit Apple Silicon / AArch64)
* `bndtools.gtk.linux.x86_64.bndrun` for running on Linux (64-bit Intel x86).
* `bndtools.gtk.linux.x86.bndrun` for running on Linux (32-bit Intel x86).
* `bndtools.win32.x86.bndrun` for running on Win32 (XP, Vista etc).

Right click on the file that matches your computer's architecture and select "Run As" > "Bnd OSGi Run Launcher". If none of these files matches the architecture you want to run on, then please create a new one and submit it back as a patch.

## Manually Building Bndtools

In addition to the way with the pre-configured development environments there is also the more manual way.

Note: Bndtools is built with Bndtools! If you want to work on the bndtools source code, you have three options:

* Install the a release of bndtools from the [Installation page](https://bndtools.org/installation.html) and start working straight away.
* Build Bndtools from the command line, then install the build results into your Eclipse IDE.

### Checking Out from GitHub

First check out the source code from GitHub as follows:

	git clone git://github.com/bndtools/bnd.git

If you have Bndtools installed in your Eclipse IDE already (e.g. using Marketplace) then skip to **Importing Into Eclipse** below. Otherwise read on...

### Building from the command-line

Assuming you have Gradle (version 1.11 or better) installed, you can build bndtools from the command line by changing to the root of your checkout and typing:

	./gradlew :build

or skip test execution for faster local builds

	./gradlew build -x test -x testOSGi

After a a short while the directory - `org.bndtools.p2/generated/p2` appears. It contains an Eclipse P2 Update Site that you can use to install bndtools from the code you have just built.


To install from the generated Update Sites, open the Help menu in Eclipse and select "Install New Software". In the update dialog, click the "Add" button (near the top left) and then click the "Local" button. Browse to the location of the `org.bndtools.p2/generated/p2` directory that you just built. Then set the name of this update site to "Bndtools Local Snapshot" (or whatever you like, it's not really important so long as you enter *something*). Click "OK".

Back in the update dialog, Bndtools will appear in the category list. Place a check next to it and click Next. Drive the rest of the wizard to completion... congratulations, you have just built and installed bndtools!

We recommend the section [Build Environment](#build-environment) below to learn more about how the build works.

## Running single tests

Sometimes it can be useful to run a single testcase without running a full build. 

- Running only a specific Test - e.g. runs the test class `biz.aQute.launcher.AlsoLauncherTest.java` in the bundle `biz.aQute.bndall.tests`

	```bash
	./gradlew :biz.aQute.bndall.tests:test --tests "biz.aQute.launcher.AlsoLauncherTest"
 	```

- Running single test method - e.g. runs the method `testTester()` in test class `biz.aQute.launcher.AlsoLauncherTest.java` in the bundle `biz.aQute.bndall.tests`

	```bash
	./gradlew :biz.aQute.bndall.tests:test --tests "biz.aQute.launcher.AlsoLauncherTest.testTester"
 	```

### Importing Into Eclipse

Now you have Bndtools installed in your Eclipse IDE, you can import the bndtools projects into Eclipse to begin working on the source code.

Open the File menu and select "Import" and then "Existing Projects into Workspace" (under the General category). Click "Next". Click the "Browse" button (top right) and select the root directory of the bndtools projects.

Ensure that all projects (sub-directories) are checked.

NB: These projects must all be in the same directory!

Click "Finish"... Eclipse will start to import and build the projects. **If you see a dialog during the import prompting you to "Create a Bnd Configuration Project" click CANCEL.**

You should now have all the bndtools projects in your workspace, ready to begin hacking!


## Build Environment

The only thing you need to build Bnd/Bndtools is Java.
- We require at least Java 17 locally installed in path.
- For a complete log file attach to the commands below `2>&1 | tee "build_$(date +%Y%m%d_%H%M%S).log"`
- We use Gradle and Maven to build and the repo includes `gradlew` and `mvnw` wrappers with the necessary versions.

- assembles and tests the Bnd Workspace projects
  ```bash
  ./gradlew build
  ```
- alternative skip tests for faster local builds 
  ```bash
  ./gradlew build -x test -x testOSGi
  ```
**MIND: Above step is pre-requisite for following build of Bnd Maven and Gradle plugin.**
- assembles and tests the Bnd Gradle plugins
  ```bash
  ./gradlew :gradle-plugins:build
  ```
- assembles and tests the Bnd Maven plugins
  ```bash
  ./mvnw install
  ```
- assembles and publishes the Bnd Workspace projects into `dist/bundles`
  ```bash
  ./gradlew publish
  ```
- assembles and publishes the Bnd Gradle plugins into `dist/bundles`
  ```bash
  ./gradlew :gradle-plugins:publish
  ```
- assembles and publishes the Bnd Maven plugins into `dist/bundles`
  ```bash
  ./mvnw -Pdist deploy
  ```

Rebuilding: bnd is built with bnd. For that reason we rebuild and retest bnd with the build we just built.
To do a full build-rebuild cycle (like the github build), you can use the following command:

```
./gradlew build \
  && ./gradlew :gradle-plugins:build \
  && ./.github/scripts/rebuild-build.sh \
  && ./.github/scripts/rebuild-test.sh \
  2>&1 | tee "build_full_$(date +%Y%m%d_%H%M%S).log"
```

We use [GitHub Actions](https://github.com/bndtools/bnd/actions?query=workflow%3A%22CI%20Build%22) for continuous integration and the repo includes a `.github/workflows/cibuild.yml` file to build via GitHub Actions.

We use [CodeQL](https://github.com/bndtools/bnd/security/code-scanning?query=tool%3ACodeQL) for continuous security analysis. Pull requests are automatically code scanned.

### Gradle Wrapper

bnd uses [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).
To update the gradle wrapper locally to a new gradle version (e.g. to test building on a higher JDK) just run the following command:

`gradle wrapper --gradle-version X.XX` (replace X.XX with your gradle version)

This generates new gradle wrapper files.
Additionally consider adding the version to `gradle-plugins/biz.aQute.bnd.gradle/src/test/groovy/aQute/bnd/gradle/TestHelper.groovy` in the method `gradleVersion()`.

If you think this new gradle wrapper might be worth a contribution to bnd, feel free to open a PR.

## Running JUnit Tests


The project `biz.aQute.tester.test` contains the unit tests for `biz.aQute.tester` and `biz.aQute.tester.junit-platform`.
See this project's [README](https://github.com/bndtools/bnd/blob/master/biz.aQute.tester.test/readme.md) for instructions of how to execute tests in Eclipse.


## Workflow

We use [git triangular workflow](https://github.blog/2015-07-29-git-2-5-including-multiple-worktrees-and-triangular-workflows/).
This means that no one, not even the Bnd/Bndtools maintainers, push contributions directly into the [main Bnd/Bndtools repo](https://github.com/bndtools/bnd).
All contributions come in through pull requests.
So each contributor will need to [fork the main Bnd/Bndtools repo](https://github.com/bndtools/bnd/fork) on GitHub.
All contributions are made as commits to your fork.
Then you submit a pull request to have them considered for merging into the main Bnd/Bndtools repo.

### Setting up the triangular workflow

After forking the main Bnd/Bndtools repo on GitHub, you can clone the main repo to your system:

    git clone https://github.com/bndtools/bnd.git

This will clone the main repo to a local repo on your disk and set up the `origin` remote in Git.
Next you will set up the the second side of the triangle to your fork repo.

    cd bnd
    git remote add fork git@github.com:github-user/bnd.git

Make sure to replace the URL with the SSH URL to your fork repo on GitHub.
Then we configure the local repo to push your commits to the fork repo.

    git config remote.pushdefault fork

So now you will pull from `origin`, the main repo, and push to `fork`, your fork repo.
This option requires at least Git 1.8.4. It is also recommended that you configure

    git config push.default simple

unless you are already using Git 2.0 where it is the default.

Finally, the third side of the triangle is pull requests from your fork repo to the
main repo.

## Contribution guidelines

### Pull requests are always welcome

We are always thrilled to receive pull requests, and do our best to process them as fast as possible.
Not sure if that typo is worth a pull request?
Do it!
We will appreciate it.

If your pull request is not accepted on the first try, don't be discouraged!
If there's a problem with the implementation, hopefully you received feedback on what to improve.

We're trying very hard to keep Bnd/Bndtools lean and focused.
We don't want it to do everything for everybody.
This means that we might decide against incorporating a new feature.
However, there might be a way to implement that feature *on top of* Bnd/Bndtools.

### Create issues

Any significant improvement should be documented as [a GitHub issue](https://github.com/bndtools/bnd/issues) before anybody starts working on it.

### But check for existing issues first

Please take a moment to check that an issue doesn't already exist documenting your bug report or improvement proposal.
If it does, it never hurts to add a quick "+1" or "I have this problem too".
This will help prioritize the most common problems and requests.

### Conventions

See [Bndtools Development: Tips and Tricks](DEV_README.md) for more detailed information, but the following are the basics.

Fork the repo and make changes on your fork in a feature branch:

- If it's a bugfix branch, name it XXX-something where XXX is the number of the issue
- If it's a feature branch, create an enhancement issue to announce your intentions, and name it XXX-something where XXX is the number of the issue.

Submit unit tests for your changes.
We use `junit` and most projects already have a number of test cases.
The test cases for `biz.aQute.bndlib` are however in the `biz.aQute.bndlib.tests` project for historical reasons.
Take a look at existing tests for inspiration.
Run the full build including all the tests in your branch before submitting a pull request.

Write clean code.
Universally formatted code promotes ease of writing, reading, and maintenance.
We use Eclipse and all the projects have Eclipse `.settings` which will properly format the code.
Make sure to avoid unnecessary white space changes which complicate diffs and make reviewing pull requests much more time consuming.

Pull requests descriptions should be as clear as possible and include a reference to all the issues that they address.

Pull requests must not contain commits from other users or branches.

Commit messages must start with a short summary (max. 50 chars) written in the imperative, followed by an optional, more detailed explanatory text which is separated from the summary by an empty line.

    index: Remove absolute URLs from the OBR index

    The url for the root was missing a trailing slash. Using File.toURI to
    create an acceptable url.

Code review comments may be added to your pull request.
Discuss, then make the suggested modifications and push the amended commits to your feature branch.
Be sure to post a comment after pushing.
The new commits will show up in the pull request automatically, but the reviewers may not be notified unless you comment.

Before the pull request is merged, make sure that you squash your commits into logical units of work using `git rebase -i` and `git push --force`.
After every commit, the test suite should be passing.
Include documentation changes in the same commit so that a revert would remove all traces of the feature or fix.

Commits that fix or close an issue should include a reference like `Closes #XXX` or `Fixes #XXX`, which will automatically close the issue when merged.

### Large changes/Work-In-Progress

Sometimes for big changes/feature additions, you may wish to submit a pull request before it is fully ready to merge, in order to solicit feedback from the core developers and ensure you're on the right track before proceeding too far.
In this case, you can submit a pull request and mark it as a Draft (see [draft pull requests](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/about-pull-requests#draft-pull-requests))

Once your pull request is ready for consideration to merge, submit a request for a review.
While the pull request is flagged as draft the maintainers are unlikely to know that it is ready, the review process won't start and your branch won't get merged.

### Sign your work

The sign-off is a simple line at the end of the commit message which certifies that you wrote it or otherwise have the right to pass it on as an open-source patch.
The rules are pretty simple: if you can certify the below (from [developercertificate.org](https://developercertificate.org/)):

    Developer Certificate of Origin
    Version 1.1

    Copyright (C) 2004, 2006 The Linux Foundation and its contributors.
    1 Letterman Drive
    Suite D4700
    San Francisco, CA, 94129

    Everyone is permitted to copy and distribute verbatim copies of this
    license document, but changing it is not allowed.

    Developer's Certificate of Origin 1.1

    By making a contribution to this project, I certify that:

    (a) The contribution was created in whole or in part by me and I
        have the right to submit it under the open source license
        indicated in the file; or

    (b) The contribution is based upon previous work that, to the best
        of my knowledge, is covered under an appropriate open source
        license and I have the right under that license to submit that
        work with modifications, whether created in whole or in part
        by me, under the same open source license (unless I am
        permitted to submit under a different license), as indicated
        in the file; or

    (c) The contribution was provided directly to me by some other
        person who certified (a), (b) or (c) and I have not modified
        it.

    (d) I understand and agree that this project and the contribution
        are public and that a record of the contribution (including all
        personal information I submit with it, including my sign-off) is
        maintained indefinitely and may be redistributed consistent with
        this project or the open source license(s) involved.

then you just add a line to end of the git commit message:

    Signed-off-by: Joe Smith <joe.smith@email.com>

using your real name. Sorry, no pseudonyms or anonymous contributions.

Many Git UI tools have support for adding the `Signed-off-by` line to the end of your commit message.
This line can be automatically added by the `git commit` command by using the `-s` option.

### Merge approval

The Bnd/Bndtools maintainers will review your pull request and, if approved, will merge into the main repo.

If your pull request was originally a work-in-progress, don't forget to remove WIP from its title to signal to the maintainers that it is ready for review.

### How can I become a maintainer

1. Learn the code inside out.
2. Make yourself useful by contributing code, bug fixes, support etc.
3. Introduce your self to the other maintainers.

Don't forget: being a maintainer is a time investment.
Make sure you will have time to make yourself available.
You don't have to be a maintainer to make a difference on the project!
