import java.util.jar.*;

def bsn = 'jar-test-bnd-process-skipIfEmpty'

// Check the bundles exist!
File bundle = new File(basedir, "target/${bsn}-0.0.1.jar")
assert !bundle.exists()
