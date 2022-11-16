// Check the bndrun file exist!
File bndrunFile = new File(basedir, 'test.bndrun')
assert bndrunFile.isFile()

// Capture input file info
context.put("timestamp", bndrunFile.lastModified())
context.put("length", bndrunFile.length())
