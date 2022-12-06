//
// The export-bundles-only case
//

// Check the bundle exist!
File bundle = new File(basedir, "target/export/test/test.jar")
assert !bundle.exists()

File targetDir = new File(basedir, "target/export/test")
assert 1 == targetDir.listFiles(new FileFilter() { boolean accept(File file) {return file.isFile();}}).length
bundle = new File(basedir, "target/export/test/org.apache.felix.eventadmin-1.4.6.jar")
assert bundle.exists()
