import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Manifest

import static org.assertj.core.api.Assertions.assertThat

File mr_bundle = new File(basedir, 'target/mr-0.0.1-SNAPSHOT.jar')
File target_directory = new File(basedir, 'target/classes')
assertThat(mr_bundle).isFile()
assertThat(target_directory).isDirectory()

JarFile mr_jar = new JarFile(mr_bundle)
//check that all manifests are there
JarEntry java9Entry = mr_jar.getEntry('META-INF/versions/9/OSGI-INF/MANIFEST.MF')
JarEntry java11Entry = mr_jar.getEntry('META-INF/versions/11/OSGI-INF/MANIFEST.MF')
assert java9Entry != null
assert java11Entry != null

//check that a main OSGi Manifest was added with the wanted headers
Attributes default_manifest = mr_jar.getManifest().getMainAttributes()
assert default_manifest.getValue('Bundle-SymbolicName') == 'mr'
assert default_manifest.getValue('Multi-Release') == 'true'
assert default_manifest.getValue('Require-Capability').contains('osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"')
assert default_manifest.getValue('Import-Package').contains('org.apache.commons.io;version=')

//check no unwanted module-info is there
assert mr_jar.getEntry('module-info.class') == null
assert mr_jar.getEntry('META-INF/versions/9/module-info.class') == null
assert mr_jar.getEntry('META-INF/versions/11/module-info.class') == null

//check the manifests itself
Attributes java9_manifest = new Manifest(mr_jar.getInputStream(java9Entry)).getMainAttributes();
assert java9_manifest.getValue('Require-Capability').contains('osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=9))"')
assert !java9_manifest.getValue('Import-Package').contains('org.apache.commons.io')

Attributes java11_manifest = new Manifest(mr_jar.getInputStream(java11Entry)).getMainAttributes();
assert java11_manifest.getValue('Require-Capability').contains('osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=11))"')
assert java11_manifest.getValue('Import-Package').contains('java.net.http')
assert !java11_manifest.getValue('Import-Package').contains('org.apache.commons.io')

true // success
