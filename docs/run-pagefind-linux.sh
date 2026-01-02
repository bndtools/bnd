#!/bin/bash
set -ev

export BUNDLE_GEMFILE=$PWD/Gemfile
bundle install --jobs=3 --retry=3
bundle exec jekyll clean
bundle exec jekyll build

# Run and serve the _site folder with search working
# install for MacOS aarch64
[ -f pagefind.tar.gz ] || curl -L https://github.com/CloudCannon/pagefind/releases/download/v1.3.0/pagefind-v1.3.0-x86_64-unknown-linux-musl.tar.gz -o pagefind.tar.gz
tar xzf pagefind.tar.gz
chmod +x pagefind
# ensure the archived _site/releases docs from former release do not get indexed by pagefind
# by removing the 'data-pagefind-body' from those pages
# see https://pagefind.app/docs/indexing/#removing-pages-from-pagefinds-index
find _site/releases -type f -name "*.html" -exec sed -i 's/data-pagefind-body//g' {} +
./pagefind --site _site --serve --output-subdir pagefindindex