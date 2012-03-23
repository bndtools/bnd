#!/bin/bash

cd out/site
runhaskell site.hs server 8000
cd ../..
