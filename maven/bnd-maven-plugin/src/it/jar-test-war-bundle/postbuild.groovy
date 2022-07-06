import groovy.xml.XmlSlurper

import java.util.jar.Attributes
import java.util.jar.JarFile

import static org.assertj.core.api.Assertions.assertThat

def bsn = 'jar-test-war-bundle'
def version = '0.0.1-SNAPSHOT'

// Check the output exists!
File wab = new File(basedir, "target/${bsn}-${version}.war")
File webapp_directory = new File(basedir, "target/${bsn}-${version}")
assertThat(wab).isFile()
assertThat(webapp_directory).isDirectory()

// Load manifests
JarFile war = new JarFile(wab)
Attributes manifest = war.getManifest().getMainAttributes()

// Basic manifest check
assert manifest.getValue('Bundle-Name') == 'Test War Bundle'
assert manifest.getValue('Bundle-Version') == '0.0.1.SNAPSHOT'
assert manifest.getValue('Bundle-ClassPath') == 'WEB-INF/classes,WEB-INF/lib/jar-test-api-bundle-0.0.1.jar'
assert manifest.getValue('Bnd-LastModified') == null

// Check contents
assert war.getEntry('WEB-INF/') != null
assert war.getEntry('WEB-INF/classes/org/example/impl/') != null
assert war.getEntry('WEB-INF/lib/jar-test-api-bundle-0.0.1.jar') != null
assert war.getEntry('WEB-INF/lib/osgi.cmpn-6.0.0.jar') == null
assert war.getEntry('WEB-INF/lib/osgi.annotation-6.0.1.jar') == null
assertThat(new File(webapp_directory, 'META-INF/MANIFEST.MF')).isFile()
assertThat(new File(webapp_directory, 'WEB-INF')).isDirectory()
assertThat(new File(webapp_directory, 'WEB-INF/classes/org/example/impl')).isDirectory()
assertThat(new File(webapp_directory, 'WEB-INF/lib/jar-test-api-bundle-0.0.1.jar')).isFile()
assertThat(new File(webapp_directory, 'WEB-INF/lib/osgi.cmpn-6.0.0.jar')).doesNotExist()
assertThat(new File(webapp_directory, 'WEB-INF/lib/osgi.annotation-6.0.1.jar')).doesNotExist()

def groupId = 'biz.aQute.bnd-test'

assert war.getEntry("META-INF/maven/${groupId}/${bsn}/pom.xml") != null
assert war.getEntry("META-INF/maven/${groupId}/${bsn}/pom.properties") != null

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

checkMavenPom(war, "META-INF/maven/${groupId}/${bsn}/pom.xml", groupId, bsn, version)
checkMavenProperties(war, "META-INF/maven/${groupId}/${bsn}/pom.properties", groupId, bsn, version)
