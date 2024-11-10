import java.util.jar.JarFile;
import java.util.jar.JarEntry

JarFile jar = new JarFile( new File(basedir, 'target/test-bnd-process-conditional-0.0.1-SNAPSHOT.jar'))
JarEntry entry = jar.getEntry('com/google/common/cache/Cache.class')
assert entry != null