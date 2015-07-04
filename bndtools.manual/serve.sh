#!/bin/bash

cd out/site
stack runghc -- site.hs server --host=0.0.0.0 -v
cd ../..
