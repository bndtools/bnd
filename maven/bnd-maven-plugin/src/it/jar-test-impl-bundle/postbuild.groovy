import java.time.*;
import java.time.format.*;
import java.util.jar.*;

def bsn = 'jar-test-impl-bundle'
def version = '0.0.1-SNAPSHOT'

// Check the bundles exist!
File impl_bundle = new File(basedir, "target/${bsn}-${version}.jar")
assert impl_bundle.isFile()

JarFile jar = new JarFile(impl_bundle)

JarEntry pomXml = jar.getJarEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.xml")
assert pomXml != null
assert jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.properties") != null

long time = pomXml.getTime();
String outputTimestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(time + TimeZone.getDefault().getOffset(time)),ZoneId.of("UTC")));
assert outputTimestamp == "2022-01-02T12:43:14Z"

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
