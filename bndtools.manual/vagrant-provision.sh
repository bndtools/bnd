#!/usr/bin/env bash

# NB: DO NOT RUN this manually -- it is executed by Vagrant to provision the initial software on the box.

# Haskell
echo INSTALLING ghc-7.8.4 AND cabal-install-1.20
apt-get -qq update
apt-get -qq install -y software-properties-common
add-apt-repository -y ppa:hvr/ghc
apt-get -qq update
apt-get install -y cabal-install-1.20 ghc-7.8.4
cat >> ~/.bashrc <<EOF
export PATH="~/.cabal/bin:/opt/cabal/1.20/bin:/opt/ghc/7.8.4/bin:\$PATH"
EOF
export PATH=~/.cabal/bin:/opt/cabal/1.20/bin:/opt/ghc/7.8.4/bin:$PATH
cabal update
cabal install alex happy

echo INSTALLING ZLIB
apt-get -qq install -y zlib1g-dev

echo INSTALLING HAKYLL-4.6.0.0
cabal update
cabal install --global hakyll-4.6.0.0
