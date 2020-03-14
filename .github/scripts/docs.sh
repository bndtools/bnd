#!/usr/bin/env bash
ruby --version
gem --version
gem install bundler -v '~> 2.0'
bundle --version
cd docs
./build.sh "$@"
