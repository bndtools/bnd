import java.util.jar.Attributes
import java.util.jar.JarFile;

// Check the bundles exist!
File tests_test_bundle_fragment = new File(basedir, 'target/test-bnd-process-tests-goal-fragment-0.0.1-SNAPSHOT-tests.jar')
assert tests_test_bundle_fragment.isFile()

// Load manifests
JarFile tests_test_bundle_fragment_jar = new JarFile(tests_test_bundle_fragment)
Attributes tests_test_bundle_fragment_manifest = tests_test_bundle_fragment_jar.getManifest().getMainAttributes()

// Basic manifest check
assert tests_test_bundle_fragment_manifest.getValue('Bundle-SymbolicName') == 'test-bnd-process-tests-goal-fragment-tests'
assert tests_test_bundle_fragment_manifest.getValue('Fragment-Host') == 'test-bnd-process-tests-goal-fragment'
