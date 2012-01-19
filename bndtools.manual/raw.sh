#!/bin/sh

echo Outputting Raw HTML
mkdir -p out/raw
cp -R images out/raw

sed -e 's/\$images\$/images\/tutorial\//g' tutorial.md > out/raw/tutorial.md
pandoc -f markdown -t html -s -S --toc out/raw/tutorial.md > out/raw/tutorial.html
