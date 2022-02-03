import java.util.jar.*;

def bsn = 'jar-test-bnd-process-tests-skipIfEmpty'
def moduleDir = bsn.replace('.', '-')

// Check the bundles exist!
File test_bnd_process_tests_skipIfEmpty = new File(basedir, "${moduleDir}/target/${bsn}-0.0.1.jar")
assert !test_bnd_process_tests_skipIfEmpty.exists()
File test_bnd_process_tests_skipIfEmpty_tests = new File(basedir, "${moduleDir}/target/${bsn}-0.0.1-tests.jar")
assert test_bnd_process_tests_skipIfEmpty_tests.isFile()
