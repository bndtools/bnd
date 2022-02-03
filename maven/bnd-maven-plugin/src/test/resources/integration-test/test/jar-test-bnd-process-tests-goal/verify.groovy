import java.util.jar.*;

def bsn = 'jar-test-bnd-process-tests-goal'
def moduleDir = bsn.replace('.', '-')

// Check the bundles exist!
File bundle = new File(basedir, "${moduleDir}/target/${bsn}-0.0.1-SNAPSHOT.jar")
assert bundle.isFile()
File tests_test_bundle = new File(basedir, 'test-bnd-process-tests-goal/target/test-bnd-process-tests-goal-0.0.1-SNAPSHOT-tests.jar')
assert tests_test_bundle.isFile()

// Load manifests
JarFile tests_main_jar = new JarFile(bundle)
Attributes tests_main_manifest = tests_main_jar.getManifest().getMainAttributes()
JarFile tests_test_jar = new JarFile(tests_test_bundle)
Attributes tests_test_manifest = tests_test_jar.getManifest().getMainAttributes()

// Basic manifest check
assert tests_main_manifest.getValue('Bundle-SymbolicName') == bsn
assert tests_test_manifest.getValue('Bundle-SymbolicName') == 'test-bnd-process-tests-goal-tests'

// Check bnd properties
assert tests_main_manifest.getValue('Test-Cases') == null
assert tests_test_manifest.getValue('Test-Cases') == 'org.example.test.ExampleTest'

// Check contents
assert tests_test_jar.getEntry('org/example/test/') != null
assert tests_test_jar.getEntry('org/example/impl/') == null
assert tests_test_jar.getEntry('org/junit/') != null
