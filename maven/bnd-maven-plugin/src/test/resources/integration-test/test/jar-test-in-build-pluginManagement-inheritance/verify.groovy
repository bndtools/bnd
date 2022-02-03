import java.util.jar.*;

def bsn = 'test-inheriting-api-bundle'
def moduleDir = "test-in-build-pluginManagement-inheritance/${bsn}"

// Check the bundles exist!
File in_build_pluginManagement_api_bundle = new File(basedir, "${moduleDir}/target/${bsn}-0.0.1.jar")
assert in_build_pluginManagement_api_bundle.isFile()

// Load manifests
JarFile in_build_pluginManagement_api_jar = new JarFile(in_build_pluginManagement_api_bundle)
Attributes in_build_pluginManagement_api_manifest = in_build_pluginManagement_api_jar.getManifest().getMainAttributes()

// Basic manifest check
assert in_build_pluginManagement_api_manifest.getValue('Bundle-SymbolicName') == "biz.aQute.bnd-test.${bsn}"
assert in_build_pluginManagement_api_manifest.getValue('Bundle-Version') == '0.0.1'
assert in_build_pluginManagement_api_manifest.getValue('Bnd-LastModified') != null

// Check inheritance of properties in bnd.bnd from the parent project
assert in_build_pluginManagement_api_manifest.getValue('X-ParentProjectProperty') == 'overridden'
assert in_build_pluginManagement_api_manifest.getValue('X-ParentProjectProperty2') == 'it worked'
