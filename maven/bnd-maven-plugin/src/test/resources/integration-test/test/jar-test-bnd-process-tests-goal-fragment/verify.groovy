import java.util.jar.*;

def bsn = 'jar-test-bnd-process-tests-goal-fragment'
def moduleDir = bsn.replace('.', '-')

// Check the bundles exist!
File bundle = new File(basedir, "${moduleDir}/target/${bsn}-0.0.1-SNAPSHOT-tests.jar")
assert bundle.isFile()

// Load manifests
JarFile jar = new JarFile(bundle)
Attributes manifest = jar.getManifest().getMainAttributes()

// Basic manifest check
assert manifest.getValue('Bundle-SymbolicName') == "${bsn}-tests"
assert manifest.getValue('Fragment-Host') == bsn
