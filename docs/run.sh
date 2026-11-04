#!/bin/bash
set -ev

export BUNDLE_GEMFILE=$PWD/Gemfile
bundle config set --local deployment false
bundle config set --local frozen false
bundle install --jobs=3 --retry=3
bash "$PWD/scripts/sync-baseline-version.sh"
bundle exec jekyll clean
bundle exec jekyll serve -w --incremental 

