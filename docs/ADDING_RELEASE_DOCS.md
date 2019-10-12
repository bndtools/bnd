## Adding Release Docs

* checkout the release tag
* edit `docs/_config.yml` setting the properties
  ```
  releasename: <version>
  baseurl: /releases/<version>
  ```
  replacing `<version>` for the actual version
* run `./build.sh`
* copy the generated contents of `docs/_site` into a temporary location
* checkout the master branch
* copy previously saved content into `docs/release/<version>`
* commit & push

**Note:** Make sure to leave `baseurl` unset in the `master` branch.