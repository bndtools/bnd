import aQute.bnd.build.Workspace
import aQute.bnd.build.model.BndEditModel
import aQute.bnd.osgi.Processor
import aQute.bnd.properties.Document
import aQute.bnd.version.MavenVersion
import aQute.lib.io.IO

import java.util.jar.Attributes
import java.util.jar.JarFile;

def baseVersion = new MavenVersion(bndVersion).toReleaseVersion()

// Check the bndrun file exist!
File bndrunFile = new File(basedir,"test.bndrun")
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
File bundle = new File(basedir,"target/test.jar")
assert bundle.isFile()

// Load manifest
JarFile jar = new JarFile(bundle)
Attributes manifest = jar.getManifest().getMainAttributes()

// Basic manifest check
assert manifest.getValue("Embedded-Runpath") =~ /jar\/org\.apache\.felix\.framework-5\.4\.0.jar/
assert manifest.getValue("Embedded-Runpath") =~ /jar\/biz\.aQute\.launcher-${baseVersion}\.jar/
assert manifest.getValue("Main-Class") == "aQute.launcher.pre.EmbeddedLauncher"

// Check contents
assert jar.getEntry("launcher.properties") != null
assert jar.getEntry("jar/biz.aQute.launcher-${baseVersion}.jar") != null
assert jar.getEntry("jar/org.apache.felix.eventadmin-1.4.6.jar") != null
assert jar.getEntry("jar/org.apache.felix.framework-5.4.0.jar") != null
String launcherProperties = jar.getInputStream(jar.getEntry("launcher.properties")).text
assert launcherProperties =~ /launch\.bundles=jar\/org\.apache\.felix\.eventadmin-1\.4\.6\.jar/
assert launcherProperties =~ /project\.artifactId=export/
