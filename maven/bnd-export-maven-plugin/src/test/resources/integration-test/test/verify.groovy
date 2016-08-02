import java.util.jar.*;

println "basedir ${basedir}"
println "bndVersion ${bndVersion}"
def baseVersion = bndVersion - '-SNAPSHOT'

// Check the bundle exist!
File bundle = new File(basedir, 'target/test.jar')
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
