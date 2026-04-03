# Patch Release Process (MICRO Version Increase)

This section describes the process for creating a patch release (MICRO version increase), such as releasing 7.2.1 after 7.2.0 has already been released.

## When to Use Patch Releases

Patch releases are used when:
- The previous MINOR version (e.g., 7.2.0) has already been released and published to Maven Central
- Critical bug fixes or security patches need to be released without introducing new features
- The fixes are cherry-picked from the `master` branch to the `next` branch

## Key Differences from Regular Releases

Patch releases differ from regular MINOR version releases in several important ways:

1. **Master branch is NOT updated** - The `master` branch continues with its current development version (e.g., 7.3.0-SNAPSHOT)
2. **First RC builds with previous release** - The first patch RC (RC1) builds using the previous final release version (e.g., 7.2.0 for 7.2.1-RC1)
3. **Subsequent RCs build with previous RC** - RC2 and later build using the RC version range (e.g., [7.2.1-RC,7.3.0))
4. **About.java gets a patch version constant** - A constant like `_7_2_1` is added (with three components, not two)

## Preparation for First Patch Release Candidate

The `next` branch should still be on the previous release version (e.g., 7.2.0). Make sure to cherry-pick the necessary fixes from `master` to `next`.

```bash
git checkout next
git cherry-pick <commit-sha>...
```

Execute the `release.sh` script with the `patch-first-rc` mode:

```bash
# Prepare next branch for 7.2.1-RC1 (assuming 7.2.0 was previously released)
.github/scripts/release.sh --mode patch-first-rc --release-version 7.2.1 --next-version 7.3.0 --rc 1
```

### What the Script Does for patch-first-rc

The script will make the following changes:

1. **Update `gradle.properties` (root)**: Set `bnd_version=[7.2.0,7.3.0)` 
   - This uses the previous final release (7.2.0), NOT the RC range
   - This is because RC1 doesn't exist yet in any repository

2. **Update `cnf/build.bnd`**: 
   - Set `base.version: 7.2.1`
   - Uncomment and set `-snapshot: RC1`

3. **Update `biz.aQute.bndlib/src/aQute/bnd/osgi/About.java`**:
   - Add new version constant: `public static final Version _7_2_1 = new Version(7, 2, 1);`
   - Update `CURRENT` to point to `_7_2_1`
   - Add `CHANGES_7_2_1` constant with link to changes wiki page
   - Add entry to CHANGES map

4. **Update `biz.aQute.bndlib/src/aQute/bnd/osgi/package-info.java`**: Set `@Version("7.2.1")`

5. **Update `maven-plugins/bnd-plugin-parent/pom.xml`**: Set `<revision>7.2.1-RC1</revision>`

6. **Update `gradle-plugins/gradle.properties`**: Set `bnd_version: 7.2.1-RC1`

7. **Update `gradle-plugins/README.md`**: Replace version references from 7.3.0 to 7.2.1

8. **Create `biz.aQute.bndlib/src/aQute/bnd/build/7.2.1.bnd`** with version defaults

### Next Steps After patch-first-rc

Follow the output instructions:

```bash
# Review changes
git diff

# Commit
git add . && git commit -m 'build: Build Release 7.2.1.RC1'

# Tag
git tag -s 7.2.1.RC1

# Push (force push to update next branch)
git push --force origin next 7.2.1.RC1
```

After pushing:
- Update JFROG manually (see main release process documentation)
- The [update-rc](https://bndtools.jfrog.io/ui/admin/repositories/virtual/update-rc/edit) P2 repository must be changed to point to the RC P2 artifact
- Set **Local Repository** to `libs-release-local` and **Path Suffix** to `org/bndtools/org.bndtools.p2/7.2.1-RC1/org.bndtools.p2-7.2.1-RC1.jar!`

## Subsequent Patch Release Candidates (RC2, RC3, ...)

When additional fixes need to be included, cherry-pick them from `master` to `next` and create a new RC:

```bash
git checkout next
git cherry-pick <commit-sha>...
```

Execute the `release.sh` script with the `patch-next-rc` mode:

```bash
# Update to 7.2.1-RC2
.github/scripts/release.sh --mode patch-next-rc --release-version 7.2.1 --next-version 7.3.0 --rc 2
```

### What the Script Does for patch-next-rc

The script will make the following changes:

1. **Update `gradle.properties` (root)**: Set `bnd_version=[7.2.1-RC,7.3.0)`
   - This now uses the RC range to pick up previous RCs (7.2.1-RC1)
   - This is the key difference from RC1

2. **Update `cnf/build.bnd`**: Set `-snapshot: RC2`

3. **Update `maven-plugins/bnd-plugin-parent/pom.xml`**: Set `<revision>7.2.1-RC2</revision>`

4. **Update `gradle-plugins/gradle.properties`**: Set `bnd_version: 7.2.1-RC2`

### Next Steps After patch-next-rc

Follow the output instructions:

```bash
# Review changes
git diff

# Commit
git add . && git commit -m 'build: Build Release 7.2.1.RC2'

# Tag
git tag -s 7.2.1.RC2

# Push
git push origin next 7.2.1.RC2
```

After pushing:
- Update JFROG for the build
- The [update-rc](https://bndtools.jfrog.io/bndtools/update-rc/) P2 repository must be changed to point to the new RC P2 artifact
- Update **Path Suffix** to `org/bndtools/org.bndtools.p2/7.2.1-RC2/org.bndtools.p2-7.2.1-RC2.jar!`
- Make sure NOT to remove the previous 7.2.1 RC releases from the virtual repository configuration

## Final Patch Release

Once the last RC has been approved, use the regular `release` mode:

```bash
# Final release of 7.2.1
.github/scripts/release.sh --mode release --release-version 7.2.1
```

This is identical to the final release process for regular releases. See the main [Release Process](https://github.com/bndtools/bnd/wiki/Release-Process#release) documentation for the complete steps.

## Summary: Patch Release Workflow

Here's a complete example workflow for releasing 7.2.1 as a patch to 7.2.0:

```bash
# Assuming 7.2.0 was already released, and next branch is on 7.2.0
git checkout next

# Cherry-pick fixes from master
git cherry-pick <fix1-sha> <fix2-sha> ...

# Prepare first patch RC
.github/scripts/release.sh --mode patch-first-rc --release-version 7.2.1 --next-version 7.3.0 --rc 1
git add . && git commit -m 'build: Build Release 7.2.1.RC1'
git tag -s 7.2.1.RC1
git push --force origin next 7.2.1.RC1
# Update JFROG manually

# If more fixes needed, prepare RC2
git cherry-pick <fix3-sha> ...
.github/scripts/release.sh --mode patch-next-rc --release-version 7.2.1 --next-version 7.3.0 --rc 2
git add . && git commit -m 'build: Build Release 7.2.1.RC2'
git tag -s 7.2.1.RC2
git push origin next 7.2.1.RC2
# Update JFROG manually

# Final release
.github/scripts/release.sh --mode release --release-version 7.2.1
git add . && git commit -m 'build: Build Release 7.2.1'
git tag -s 7.2.1
git push origin next 7.2.1
# Follow post-release steps (Maven Central, GitHub Release, etc.)
```

## Important Notes

1. **Master branch is NOT touched** - Unlike regular releases, patch releases do not update the `master` branch at all. The `master` branch continues with the next development version (e.g., 7.3.0-SNAPSHOT).

2. **RC1 uses base version** - The first patch RC builds with the previous final release version (e.g., `bnd_version=[7.2.0,7.3.0)`) because the RC doesn't exist yet.

3. **RC2+ uses RC range** - Subsequent patch RCs use the RC version range (e.g., `bnd_version=[7.2.1-RC,7.3.0)`) to pick up previous RCs.

4. **About.java patch constant** - The patch version constant includes all three components (e.g., `_7_2_1`), unlike regular releases which only use two (e.g., `_7_2`).

5. **Wiki page** - Create a new wiki page for the patch release: `Changes-in-7.2.1`

6. **Post-release process** - The post-release process (Maven Central, GitHub Release, JFROG updates, etc.) is the same as for regular releases.
