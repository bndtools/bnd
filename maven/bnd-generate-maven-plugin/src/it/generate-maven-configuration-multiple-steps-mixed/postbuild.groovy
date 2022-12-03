// Check the ecore file exist!
File ecore = new File(basedir,"src/main/resources/model/test.ecore")
assert ecore.isFile()

// Check the genmodel file exist!
File genmodel = new File(basedir,"src/main/resources/model/test.genmodel")
assert genmodel.isFile()

// Check the if some code generated file exist!
File testClass = new File(basedir,"src/main/java/aQute/bnd/maven/model/test/Test.java")
assert testClass.isFile()
// Check the if the second step was done
File testClass2 = new File(basedir,"src/main/java2/aQute/bnd/maven/model/test/Test.java")
assert testClass2.isFile()

// Check the if the code from the bnd.bnd file instruction was generated
File testClass3 = new File(basedir,"src/main/java3/aQute/bnd/maven/model/test/Test.java")
assert testClass3.isFile()

