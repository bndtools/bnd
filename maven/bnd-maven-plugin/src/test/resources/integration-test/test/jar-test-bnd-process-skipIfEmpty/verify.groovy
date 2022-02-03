import java.util.jar.*;

def bsn = 'jar-test-bnd-process-skipIfEmpty'
def moduleDir = bsn.replace('.', '-')

// Check the bundles exist!
File bundle = new File(basedir, "${moduleDir}/target/${bsn}-0.0.1.jar")
assert !bundle.exists()
