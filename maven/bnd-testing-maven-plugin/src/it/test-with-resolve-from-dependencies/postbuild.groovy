import aQute.bnd.build.*;
import aQute.bnd.build.model.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;

// Check the bndrun file exist!
File bndrunFile = new File(basedir, "test.bndrun")
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

File testsuite_xml = new File(basedir, "target/test-reports/test/TEST-test-with-resolve-from-dependencies-0.0.1.xml")
assert testsuite_xml.isFile();
testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.test-with-resolve-from-dependencies'
assert testsuite.@tests == 3
assert testsuite.@errors == 0
assert testsuite.@failures == 0
