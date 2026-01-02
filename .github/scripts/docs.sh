#!/usr/bin/env bash
set -ev
cd docs
ruby --version
gem --version
bundle --version
bundle exec jekyll build

# create search index under _site/pagefind
pwd
# ensure the archived _site/releases docs from former release do not get indexed by pagefind
# by removing the 'data-pagefind-body' from those pages
# see https://pagefind.app/docs/indexing/#removing-pages-from-pagefinds-index
find _site/releases -type f -name "*.html" -exec sed -i 's/data-pagefind-body//g' {} +
./pagefind --verbose --site _site --output-subdir pagefindindex
