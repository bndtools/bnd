#!/usr/bin/env bash
set -ev
cd docs
ruby --version
gem --version
bundle --version
bundle exec jekyll build
