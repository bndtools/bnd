#!/usr/bin/env bash
set -ev
cd docs
ruby --version
gem --version
bundle --version
bash ./scripts/sync-baseline-version.sh
bundle exec jekyll build
