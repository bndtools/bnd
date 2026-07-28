## Adding Release Docs

This is the manual process done in `./github/workflows/postrelease.yml`. Keep both in sync.

* checkout the release tag
* edit `docs/_config.yml` setting the properties
  ```
  releasename: <version>
  baseurl: /releases/<version>
  ```
  replacing `<version>` for the actual version
* run `./build.sh` (this generates a `docs/_site` folder)
* run `rm -rf _site/releases` (this removes the old archived releases temporarily)
* run 
```shell
# Remove unwanted root-level files (keeping only the ones you specified)
find _site -maxdepth 1 -type f ! -name "404.html" \
  ! -name "favicon.ico" \
  ! -name "index.html" \
  ! -name "robots.txt" \
  ! -name "sitemap.xml" \
  ! -name "snapshot.html" \
  -delete
```                           
* copy the remaining generated contents of `docs/_site` into a temporary location
* checkout the master branch
* copy previously saved content into `docs/release/<version>`
* commit & push

**Note:** Make sure to leave `baseurl` unset in the `master` branch.