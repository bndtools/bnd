#!/bin/bash

# Populate site
echo Generating site
mkdir -p out
cp -R site out

mkdir -p out/site/images
cp -R images/tutorial images/concepts out/site/images
cp -R images/faq out/site/images
cp -R images/version-2.0.0 out/site/images/version-2.0.0

for name in `ls *.md`; do
	echo Processing ${name} into out/site/${name}
	sed -e 's/\$images\$/\/images\/tutorial\//g' ${name} > out/site/${name}
done

# Clean and Build Site with Hakyll
rm -r site/_site
rm -r site/_cache
cd out/site
runhaskell site.hs build
cd ../..
