#!/usr/bin/env bash
set -ev
cd docs
ruby --version
gem --version
bundle --version
bundle exec jekyll build

# create search index under _site/pagefind
pwd
./pagefind --verbose --site _site --output-subdir pagefindindex
