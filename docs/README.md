# bnd manual Web Site

This folder holds the bnd manual website hosted at [https://bnd.bndtools.org][bndmanual].
This web site is maintained as markdown and turned into HTML by [Jekyll][jekyll]
using [GitHub Pages](https://help.github.com/articles/what-are-github-pages/).

[bndmanual]: https://bnd.bndtools.org
[jekyll]: https://jekyllrb.com/

## Local docs development

```
# Start the local Jekyll Dev-Server
./run.sh
```

## Build docs

```
# Build it
./build.sh
```

## Setup local docs development environment (MacOS)

If you get an error like `activesupport-7.0.7.2 requires ruby version >= 2.7.0, which is incompatible with the current version, ruby 2.6.10p210` when executing `./run.sh` then consider doing the following:

- Install rbenv Ruby Version manager https://github.com/rbenv/rbenv e.g. via brew
- and then use it to install and use e.g. ruby 3.1.2

```
brew install rbenv
rbenv init
rbenv install 3.1.2
rbenv global 3.1.2
./run.sh
```

After a successfull start of `./run.sh` you see this:

```
Server address: http://127.0.0.1:4000
Server running... press ctrl-c to stop.
```

Open http://127.0.0.1:4000 in your browser to see the result while developing. 
The server does support hot-reload so you should see changes to `.md` files immediately without restart (there are a few exceptions). Checkout the [jekyll-docs](https://jekyllrb.com/docs/pages/) to get more into the details and features.


## Local development with Pagefind search

We use https://pagefind.app/ for our search field full text search. 
But it is currently not automatically working when using `./run.sh` above, because it works on the 
actual build-output on the `_site` folder (which contains the actual `.html` pages). 

To test the search locally based on the `_site` folder content, run:


`./run-pagefind-linux.sh`

or

`run-pagefind-macos.sh`

depending on which operating system you are using. 

The result should look like:

`Serving "_site" at http://localhost:1414`


The script will download and execute the pagefind executable binary after the build.
Then it will start a small server where you test the result. 
Note, that this is different than the `./run.sh` and does not support real-time editing of the content.

Feel free to adjust / extend the start-scripts if you have a different architecture 
or to use a different `pagefind` version. 

### pagefind for production build via github actions

See the files `.github/workflows/cibuild.yml` and `.github/scripts/docs.sh` for how 
building the site and executing `pagefind` is done in the final build on github.