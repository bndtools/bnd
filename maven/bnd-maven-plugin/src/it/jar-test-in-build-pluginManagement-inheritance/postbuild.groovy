import java.util.jar.*;
import groovy.xml.XmlSlurper


def bsn = 'jar-test-inheriting-api-bundle'
def version = '0.0.1'

// Check the bundles exist!
File bundle = new File(basedir, "${bsn}/target/${bsn}-${version}.jar")
assert bundle.isFile()

// Load manifests
JarFile jar = new JarFile(bundle)
Attributes manifest = jar.getManifest().getMainAttributes()

// Basic manifest check
assert manifest.getValue('Bundle-SymbolicName') == "biz.aQute.bnd-test.${bsn}"
assert manifest.getValue('Bundle-Version') == version
assert manifest.getValue('Bnd-LastModified') != null

// Check inheritance of properties in bnd.bnd from the parent project
assert manifest.getValue('X-ParentProjectProperty') == 'overridden'
assert manifest.getValue('X-ParentProjectProperty2') == 'it worked'

assert jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.xml") != null
assert jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.properties") != null

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
