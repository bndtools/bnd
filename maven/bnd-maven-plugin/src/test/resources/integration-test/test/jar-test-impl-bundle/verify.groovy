import java.util.jar.*;

def bsn = 'jar-test-impl-bundle'
def moduleDir = bsn.replace('.', '-')
def version = '0.0.1-SNAPSHOT'

// Check the bundles exist!
File impl_bundle = new File(basedir, "${moduleDir}/target/${bsn}-${version}.jar")
assert impl_bundle.isFile()

JarFile jar = new JarFile(impl_bundle)

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