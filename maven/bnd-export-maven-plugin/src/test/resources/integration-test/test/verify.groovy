import aQute.bnd.build.*;
import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;
import java.util.jar.*;

println "basedir ${basedir}"
println "bndVersion ${bndVersion}"
def baseVersion = bndVersion - '-SNAPSHOT'

// Check the bndrun file exist!
File bndrunFile = new File(new File(basedir, "export"), 'test.bndrun')
assert bndrunFile.isFile()

// Load the BndEditModel of the bndrun file so we can inspect the result
Processor processor = new Processor()
processor.setProperties(bndrunFile)
BndEditModel bem = new BndEditModel(Workspace.createStandaloneWorkspace(processor, bndrunFile.toURI()))
Document doc = new Document(IO.collect(bndrunFile))
bem.loadFrom(doc)

// Get the -runbundles.
def bemRunBundles = bem.getRunBundles()
assert null == bemRunBundles

// Check the bundle exist!
File bundle = new File(new File(basedir, "export"), 'target/test.jar')
assert bundle.isFile()

// Load manifest
JarFile jar = new JarFile(bundle)
Attributes manifest = jar.getManifest().getMainAttributes()

// Basic manifest check
assert manifest.getValue('Embedded-Runpath') =~ /jar\/org\.apache\.felix\.framework-5\.4\.0.jar/
assert manifest.getValue('Embedded-Runpath') =~ /jar\/biz\.aQute\.launcher-${baseVersion}\.jar/
assert manifest.getValue('Main-Class') == 'aQute.launcher.pre.EmbeddedLauncher'

// Check contents
assert jar.getEntry('launcher.properties') != null
assert jar.getEntry("jar/biz.aQute.launcher-${baseVersion}.jar") != null
assert jar.getEntry('jar/org.apache.felix.eventadmin-1.4.6.jar') != null
assert jar.getEntry('jar/org.apache.felix.framework-5.4.0.jar') != null
assert jar.getInputStream(jar.getEntry('launcher.properties')).text =~ /launch\.bundles=jar\/org\.apache\.felix\.eventadmin-1\.4\.6\.jar/

//
// The bundles-only case
//

// Check the bundle exist!
bundle = new File(new File(basedir, "export-bundles-only"), 'target/test.jar')
assert !bundle.exists()

File targetDir = new File(new File(basedir, "export-bundles-only"), 'target')
assert 1 == targetDir.listFiles().length
File bundleFile = new File(new File(basedir, "export-bundles-only"), 'target/org.apache.felix.eventadmin-1.4.6.jar')
assert bundleFile.exists()
