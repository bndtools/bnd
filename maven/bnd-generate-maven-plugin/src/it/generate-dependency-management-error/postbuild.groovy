import aQute.bnd.build.Workspace
import aQute.bnd.build.model.BndEditModel
import aQute.bnd.osgi.Processor
import aQute.bnd.properties.Document
import aQute.bnd.version.MavenVersion
import aQute.lib.io.IO

import java.util.jar.Attributes
import java.util.jar.JarFile;

def baseVersion = new MavenVersion(bndVersion).toReleaseVersion()

// Check the ecore file exist!
File ecore = new File(basedir,"src/main/resources/model/test.ecore")
assert ecore.isFile()

// Check the genmodel file exist!
File genmodel = new File(basedir,"src/main/resources/model/test.genmodel")
assert genmodel.isFile()

// Check the if some code generated file exist!
File testClass = new File(basedir,"src/main/java/aQute/bnd/maven/model/test/Test.java")
assert !testClass.isFile()

