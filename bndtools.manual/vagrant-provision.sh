#!/usr/bin/env bash

# NB: DO NOT RUN this manually -- it is executed by Vagrant to provision the initial software on the box.

# Haskell
echo INSTALLING GHC 7.6.3-10
apt-get -qq update
apt-get -qq install -y ghc=7.6.3-10

echo INSTALLING CABAL
apt-get -qq install -y cabal-install

echo INSTALLING ZLIB
apt-get -qq install -y zlib1g-dev

echo INSTALLING HAKYLL-4.6.0.0
cabal update
cabal install --global hakyll-4.6.0.0
