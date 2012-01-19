# Copy generated site to deploy directory
for name in `ls out/site/_site`; do
	echo copying out/site/_site/$name to ../../bndtools.github.com
	cp -R out/site/_site/${name} ../../bndtools.github.com
done