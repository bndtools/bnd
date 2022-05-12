import java.util.jar.*;

// Check the bundles exist!
File api_bundle = new File(basedir, 'target/test-api-bundle-0.0.1.jar')
assert api_bundle.isFile()

// Load manifests
JarFile api_jar = new JarFile(api_bundle)
Attributes api_manifest = api_jar.getManifest().getMainAttributes()

// Basic manifest check
assert api_manifest.getValue('Bundle-SymbolicName') == 'test-api-bundle'
assert api_manifest.getValue('Bundle-Name') == 'Test API Bundle'
assert api_manifest.getValue('Bundle-Version') == '0.0.1.bndqual'
assert api_manifest.getValue('Bnd-LastModified') == null

// Check inheritance of properties in bnd.bnd from the parent project
assert api_manifest.getValue('X-ParentProjectProperty') == 'it worked'

// Check -include of bnd files
assert api_manifest.getValue('X-IncludedParentProjectProperty') == 'Included via -include in parent bnd.bnd file'

// Check bnd properties
assert api_manifest.getValue('Project-Name') == 'Test API Bundle'
assert api_manifest.getValue('Project-Dir') == basedir.absolutePath.replace(File.separatorChar, '/' as char)
assert api_manifest.getValue('Project-Output') == new File(basedir, 'target').absolutePath
assert api_manifest.getValue('Project-Buildpath')
assert api_manifest.getValue('Project-Sourcepath')
assert api_manifest.getValue('Here') == basedir.absolutePath.replace(File.separatorChar, '/' as char)
assert api_manifest.getValue('Parent-Here') == new File(basedir, "../process-parent").canonicalPath.replace(File.separatorChar, '/' as char)

// Check contents
assert api_jar.getEntry('org/example/api/') != null
assert api_jar.getEntry('org/example/api/aresource.txt') != null
assert api_jar.getInputStream(api_jar.getEntry('org/example/api/aresource.txt')).text =~ /This is a resource/
assert api_jar.getEntry('org/example/types/') != null
assert api_jar.getEntry('OSGI-OPT/src/') != null
