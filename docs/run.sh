#!/bin/bash
set -ev

export BUNDLE_GEMFILE=$PWD/Gemfile
bundle install --jobs=3 --retry=3 --deployment --path=bundler
bundle exec jekyll serve -w --incremental 

