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

## Export Documentation

You can export the documentation to single HTML or PDF files for offline viewing or archiving:

```bash
# Export to single HTML file
./export-single-html.sh

# Export to PDF (requires wkhtmltopdf, weasyprint, or chromium)
./export-pdf.sh

# Export specific version
./export-single-html.sh 7.0.0
./export-pdf.sh 7.0.0
```

For more details, see [EXPORT_README.md](EXPORT_README.md).

## Setup local docs development environment (MacOS)

If you get an error like `activesupport-7.0.7.2 requires ruby version >= 2.7.0, which is incompatible with the current version, ruby 2.6.10p210` when executing `./run.sh` then consider doing the following:

- Install rbenv Ruby Version manager https://github.com/rbenv/rbenv e.g. via brew
- and then use it to install and use e.g. ruby 3.1.2

```
brew install rbenv
rbenv init
rbenv install 3.4.5
rbenv local 3.4.5
./run.sh
```

After a successfull start of `./run.sh` you see this:

```
Server address: http://127.0.0.1:4000
Server running... press ctrl-c to stop.
```

Open http://127.0.0.1:4000 in your browser to see the result while developing. 
The server does support hot-reload so you should see changes to `.md` files immediately without restart (there are a few exceptions). Checkout the [jekyll-docs](https://jekyllrb.com/docs/pages/) to get more into the details and features.


## Generating some pages for headers and instructions

The folders `_heads` and `_instructions` are special. They are populated by a bnd command `generatemanual` which generates .md files from the `Syntax.java` class.

As a developer the process is like this:

```
# build bnd (e.g. via cd..; ./gradlew :build -x test) get a recent iz.aQute.bnd.jar
# then generate the documentation by running the generatemanual CLI command (see bnd.java)
# go into the /docs folder of the bnd repo
cd bndrepo/docs
./generate.sh
```

This `generate.sh` under the hood calls the bnd CLI command `generatemanual` via: `java -jar ../biz.aQute.bnd/generated/biz.aQute.bnd.jar generatemanual .`

This creates or updates files in `/docs/_heads` and `/docs/_instructions`.

If new files were added or existing files updated, then commit them to git in a PR.
We need to do this from time to time to update the docs when new headers or instructions get added to `Syntax.java`.

### Adding content to generated files

There is a extension mechanism to add manual content and also override the frontmatter block of the markdown files.
Each folder (`/docs/_heads` and `/docs/instructions`) contains an `_ext` subfolder (ext as extension).
Please add file with the same name in that `_ext` sub-folder and then this content will be added to the bottom of the generated file. Frontmatter attributes will be overwritten too if specified.




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


### CSS Styling for Code Highlighter

- jekyll uses `rouge` code highlighter
- see `_config.yml` and section `syntax_highlighter_opts`
- also see this website https://jekyll-themes.com/brazacz/rouge-themes for examples
- rouge is compatible with and can use 'pygments' styles (see https://pygments.org/styles/)
- these styles can be generated with the command 

The following commands locally will help to generate the .css files for the pygment styles:

```
gem install rouge
# show a list of supported styles
rougify help style
# go to the css folder
cd css
# generate the css
rougify style github.dark > github.dark.css
```

Then reference the `.css` file in `_includes/head.htm `