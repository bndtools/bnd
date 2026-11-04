#!/bin/bash
set -ev

bash "$(dirname "$0")/scripts/sync-baseline-version.sh"

export BUNDLE_GEMFILE=$PWD/Gemfile
BUNDLE_DEPLOYMENT=true BUNDLE_FROZEN=true BUNDLE_PATH=vendor/bundle \
	bundle install --jobs=3 --retry=3
bundle exec jekyll build
