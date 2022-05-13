import java.util.jar.*;

// Check the bundles exist!
File war_bundle = new File(basedir, 'target/test-war-bundle-0.0.1-SNAPSHOT.war')
assert war_bundle.isFile()

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
