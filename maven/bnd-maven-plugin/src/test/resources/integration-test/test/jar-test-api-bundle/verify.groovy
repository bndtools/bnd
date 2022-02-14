import java.util.jar.*;

def bsn = 'jar.test.api.bundle'
def moduleDir = bsn.replace('.', '-')
def version = '0.0.1'

// Check the bundles exist!
File bundle = new File(basedir, "${moduleDir}/target/${bsn}-${version}.jar")
assert bundle.isFile()

// Load manifests
JarFile jar = new JarFile(bundle)
Attributes manifest = jar.getManifest().getMainAttributes()

// Basic manifest check
assert manifest.getValue('Bundle-SymbolicName') == bsn
assert manifest.getValue('Bundle-Name') == 'Test API Bundle'
assert manifest.getValue('Bundle-Version') == "${version}.bndqual"
assert manifest.getValue('Bnd-LastModified') == null

// Check inheritance of properties in bnd.bnd from the parent project
assert manifest.getValue('X-ParentProjectProperty') == 'it worked'

// Check -include of bnd files
assert manifest.getValue('X-IncludedParentProjectProperty') == 'Included via -include in parent bnd.bnd file'

// Check bnd properties
assert manifest.getValue('Project-Name') == 'Test API Bundle'
assert manifest.getValue('Project-Dir') == new File(basedir, moduleDir).absolutePath.replace(File.separatorChar, '/' as char)
assert manifest.getValue('Project-Output') == new File(basedir, "${moduleDir}/target").absolutePath
assert manifest.getValue('Project-Buildpath')
assert manifest.getValue('Project-Sourcepath')
assert manifest.getValue('Here') == new File(basedir, moduleDir).absolutePath.replace(File.separatorChar, '/' as char)
assert manifest.getValue('Parent-Here') == new File(basedir, 'jar-parent').absolutePath.replace(File.separatorChar, '/' as char)

// Check contents
assert jar.getEntry('org/example/api/') != null
assert jar.getEntry('org/example/api/aresource.txt') != null
assert jar.getInputStream(jar.getEntry('org/example/api/aresource.txt')).text =~ /This is a resource/
assert jar.getEntry('org/example/types/') != null
assert jar.getEntry('OSGI-OPT/src/') != null

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

checkMavenPom(jar, "META-INF/maven/${groupId}/${bsn}/pom.xml", groupId, bsn, version)
checkMavenProperties(jar, "META-INF/maven/${groupId}/${bsn}/pom.properties", groupId, bsn, version)
