import java.util.jar.*;

def bsn = 'jar-test-impl-bundle'
def moduleDir = bsn.replace('.', '-')

// Check the bundles exist!
File impl_bundle = new File(basedir, "${moduleDir}/target/${bsn}-0.0.1-SNAPSHOT.jar")
assert impl_bundle.isFile()

