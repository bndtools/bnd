# Copy generated site to deploy directory
if [[ ! -d ../../bndtools.github.com ]]; then
  echo "Could not find directory ../../bndtools.github.com"
  exit 1
fi


rm -fr ../../bndtools.github.com/*
for name in `ls out/site/_site`; do
	echo copying out/site/_site/$name to ../../bndtools.github.com
	cp -R out/site/_site/${name} ../../bndtools.github.com
done
