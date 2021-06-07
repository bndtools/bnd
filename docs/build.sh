#!/bin/bash
set -ev

export BUNDLE_GEMFILE=$PWD/Gemfile
bundle config --local deployment 'true'
bundle config --local path 'vendor/bundle'
bundle install --jobs=3 --retry=3
bundle exec jekyll build
