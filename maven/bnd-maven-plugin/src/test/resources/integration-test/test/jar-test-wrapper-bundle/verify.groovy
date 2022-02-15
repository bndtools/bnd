import java.util.jar.*;

def bsn = 'test.wrapper.bundle'
def moduleDir = 'jar-test-wrapper-bundle'
def version = '0.0.1-BUILD-SNAPSHOT'

// Check the bundles exist!
File bundle = new File(basedir, "${moduleDir}/target/${moduleDir}-${version}.jar")
assert bundle.isFile()

// Load manifests
JarFile jar = new JarFile(bundle)
Attributes manifest = jar.getManifest().getMainAttributes()

// Basic manifest check
assert manifest.getValue('Bundle-SymbolicName') == bsn
assert manifest.getValue('Bundle-Name') == moduleDir
assert manifest.getValue('Bundle-Version') == '0.0.1.BUILD-SNAPSHOT'
assert manifest.getValue('Bundle-ClassPath') == '.,lib/osgi.annotation.jar'
assert manifest.getValue('Bnd-LastModified') == null

// Check inheritance of properties in bnd.bnd from the parent project
assert manifest.getValue('X-ParentProjectProperty') == 'overridden'

// Check -include of bnd files
assert manifest.getValue('X-IncludedParentProjectProperty') == 'Included via -include in parent bnd.bnd file'
assert manifest.getValue('X-IncludedProperty') == 'Included via -include in project bnd.bnd file'

// Check bnd properties
assert manifest.getValue('Project-Name') == moduleDir
assert manifest.getValue('Project-Dir') == new File(basedir, moduleDir).absolutePath.replace(File.separatorChar, '/' as char)
assert manifest.getValue('Project-Output') == new File(basedir, "${moduleDir}/target").absolutePath
assert manifest.getValue('Project-Buildpath')
assert !manifest.getValue('Project-Sourcepath')
assert manifest.getValue('Parent-Here') == new File(basedir, 'jar-parent').absolutePath.replace(File.separatorChar, '/' as char)

// Check contents
assert jar.getEntry('org/example/api/') != null
assert jar.getEntry('org/example/types/') != null
assert jar.getEntry('lib/osgi.annotation.jar') != null

assert jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${moduleDir}/pom.xml") != null
assert jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${moduleDir}/pom.properties") != null

def groupId = 'biz.aQute.bnd-test'

def checkMavenPom(JarFile jar, String entry, String groupId, String artifactId, String version) {
	def pom = new XmlSlurper().parse(jar.getInputStream(jar.getEntry(entry)))
	assert pom.groupId == groupId || pom.parent.groupId == groupId
	assert pom.artifactId == artifactId
	assert pom.version == version
}

def checkMavenProperties(JarFile jar, String entry, String groupId, String artifactId, String version) {
	def properties = new Properties()
	properties.load(jar.getInputStream(jar.getEntry(entry)))
	assert properties.groupId == groupId
	assert properties.artifactId == artifactId
	assert properties.version == version
}

checkMavenPom(jar, "META-INF/maven/${groupId}/${moduleDir}/pom.xml", groupId, moduleDir, version)
checkMavenProperties(jar, "META-INF/maven/${groupId}/${moduleDir}/pom.properties", groupId, moduleDir, version)