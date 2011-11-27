#!/bin/sh

echo Outputting Wordpress HTML
mkdir out
mkdir out/wp
cp -R images out/wp
sed -e 's/IMAGES/\/wp-content\/uploads\/2011\/11/g' tutorial.md > out/wp/tutorial.md
pandoc -f markdown -t html -s -S --toc out/wp/tutorial.md > out/wp/tutorial.html
