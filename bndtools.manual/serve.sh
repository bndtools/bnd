#!/bin/bash

cd out/site
runhaskell site.hs server --host=0.0.0.0 -v
cd ../..
