import aQute.bnd.build.*;
import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;
import java.io.*;
import java.util.jar.*;

println "basedir ${basedir}"

// Check the bndrun file exist!
File bndrunFile = new File(new File(basedir, 'test-with-resolve'), "test.bndrun")
assert bndrunFile.isFile()

// Load the BndEditModel of the bndrun file so we can inspect the result
Processor processor = new Processor()
processor.setProperties(bndrunFile)
BndEditModel bem = new BndEditModel(Workspace.createStandaloneWorkspace(processor, bndrunFile.toURI()))
Document doc = new Document(IO.collect(bndrunFile))
bem.loadFrom(doc)

// Get the -runbundles.
def bemRunBundles = bem.getRunBundles()
assert null == bemRunBundles

File testsuite_xml = new File("${basedir}/test-no-resolve/target/test-reports/test/TEST-test-no-resolve-0.0.1.xml")
assert testsuite_xml.isFile();
def testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.test-no-resolve'
assert testsuite.@tests == 1
assert testsuite.@errors == 0
assert testsuite.@failures == 0

testsuite_xml = new File("${basedir}/test-with-resolve/target/test-reports/test/TEST-test-with-resolve-0.0.1.xml")
assert testsuite_xml.isFile();
testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.test-with-resolve'
assert testsuite.@tests == 2
assert testsuite.@errors == 0
assert testsuite.@failures == 0

testsuite_xml = new File("${basedir}/test-with-resolve-from-dependencies/target/test-reports/test/TEST-test-with-resolve-from-dependencies-0.0.1.xml")
assert testsuite_xml.isFile();
testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.test-with-resolve-from-dependencies'
assert testsuite.@tests == 3
assert testsuite.@errors == 0
assert testsuite.@failures == 0

testsuite_xml = new File("${basedir}/test-single-test/target/test-reports/test/TEST-test.xml")
assert testsuite_xml.isFile();
testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.run'
assert testsuite.@tests == 1
assert testsuite.@errors == 0
assert testsuite.@failures == 0
