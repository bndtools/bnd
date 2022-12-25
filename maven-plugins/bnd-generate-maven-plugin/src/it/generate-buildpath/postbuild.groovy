// Check the ok file exists!
File testGenerate = new File(basedir,"generate-test/gen-src/ok.txt")
assert testGenerate.isFile()

File generate = new File(basedir,"generate/gen-src/ok.txt")
assert generate.isFile()

