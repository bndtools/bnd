import groovy.xml.XmlSlurper

import java.util.jar.Attributes
import java.util.jar.JarFile

def bsn = 'jar-test-bnd-process-tests-goal'
def version = '0.0.1-SNAPSHOT'

// Check the bundles exist!
File bundle = new File(basedir, "target/${bsn}-${version}.jar")
assert bundle.isFile()
File tests_test_bundle = new File(basedir, "target/${bsn}-${version}-tests.jar")
assert tests_test_bundle.isFile()

// Load manifests
JarFile tests_main_jar = new JarFile(bundle)
Attributes tests_main_manifest = tests_main_jar.getManifest().getMainAttributes()
JarFile tests_test_jar = new JarFile(tests_test_bundle)
Attributes tests_test_manifest = tests_test_jar.getManifest().getMainAttributes()

// Basic manifest check
assert tests_main_manifest.getValue('Bundle-SymbolicName') == bsn
assert tests_test_manifest.getValue('Bundle-SymbolicName') == "${bsn}-tests"

// Check bnd properties
assert tests_main_manifest.getValue('Test-Cases') == null
assert tests_test_manifest.getValue('Test-Cases') == 'org.example.test.ExampleTest'

// Check contents
assert tests_test_jar.getEntry('org/example/test/') != null
assert tests_test_jar.getEntry('org/example/impl/') == null
assert tests_test_jar.getEntry('org/junit/') != null

assert tests_main_jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.xml") != null
assert tests_main_jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.properties") != null
assert tests_test_jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.xml") != null
assert tests_test_jar.getEntry("META-INF/maven/biz.aQute.bnd-test/${bsn}/pom.properties") != null

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

checkMavenPom(tests_main_jar, "META-INF/maven/${groupId}/${bsn}/pom.xml", groupId, bsn, version)
checkMavenProperties(tests_main_jar, "META-INF/maven/${groupId}/${bsn}/pom.properties", groupId, bsn, version)

checkMavenPom(tests_test_jar, "META-INF/maven/${groupId}/${bsn}/pom.xml", groupId, bsn, version)
checkMavenProperties(tests_test_jar, "META-INF/maven/${groupId}/${bsn}/pom.properties", groupId, bsn, version)
