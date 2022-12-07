import java.util.jar.Attributes
import java.util.jar.JarFile

import static org.assertj.core.api.Assertions.assertThat

// Check the output exists!
File war_bundle = new File(basedir, 'target/test-war-bundle-0.0.1-SNAPSHOT.war')
File webapp_directory = new File(basedir, 'target/test-war-bundle-0.0.1-SNAPSHOT')
assertThat(war_bundle).isFile()
assertThat(webapp_directory).isDirectory()

// Load manifests
JarFile war_jar = new JarFile(war_bundle)
Attributes war_manifest = war_jar.getManifest().getMainAttributes()

// Basic manifest check
assert war_manifest.getValue('Bundle-Name') == 'Test War Bundle'
assert war_manifest.getValue('Bundle-Version') == '0.0.1.SNAPSHOT'
assert war_manifest.getValue('Bundle-ClassPath') == 'WEB-INF/classes,WEB-INF/lib/test-api-bundle-0.0.1.jar'
assert war_manifest.getValue('Bnd-LastModified') == null

// Check contents
assert war_jar.getEntry('WEB-INF/') != null
assert war_jar.getEntry('WEB-INF/classes/org/example/impl/') != null
assert war_jar.getEntry('WEB-INF/lib/test-api-bundle-0.0.1.jar') != null
assert war_jar.getEntry('WEB-INF/lib/osgi.cmpn-6.0.0.jar') == null
assert war_jar.getEntry('WEB-INF/lib/osgi.annotation-6.0.1.jar') == null

assertThat(new File(webapp_directory, 'META-INF/MANIFEST.MF')).isFile()
assertThat(new File(webapp_directory, 'WEB-INF')).isDirectory()
assertThat(new File(webapp_directory, 'WEB-INF/classes/org/example/impl')).isDirectory()
assertThat(new File(webapp_directory, 'WEB-INF/lib/test-api-bundle-0.0.1.jar')).isFile()
assertThat(new File(webapp_directory, 'WEB-INF/lib/osgi.cmpn-6.0.0.jar')).doesNotExist()
assertThat(new File(webapp_directory, 'WEB-INF/lib/osgi.annotation-6.0.1.jar')).doesNotExist()

true // success
