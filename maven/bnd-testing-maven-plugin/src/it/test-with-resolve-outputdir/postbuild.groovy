import aQute.bnd.build.Workspace
import aQute.bnd.build.model.BndEditModel
import aQute.bnd.osgi.Processor
import aQute.bnd.properties.Document
import aQute.lib.io.IO
import groovy.xml.XmlSlurper

File bndrunFile = new File(basedir, 'test.bndrun')
assert context.get("timestamp") == bndrunFile.lastModified()
assert context.get("length") == bndrunFile.length()

// Check the output bndrun file exist!
bndrunFile = new File(basedir,"target/test.bndrun")
assert bndrunFile.isFile()

// Load the BndEditModel of the bndrun file so we can inspect the result
Processor processor = new Processor()
processor.setProperties(bndrunFile)
BndEditModel bem = new BndEditModel(Workspace.createStandaloneWorkspace(processor, bndrunFile.toURI()))
Document doc = new Document(IO.collect(bndrunFile))
bem.loadFrom(doc)

// Get the -runbundles.
def bemRunBundles = bem.getRunBundles()
assert bemRunBundles
assert bemRunBundles.size() >= 3

File testsuite_xml = new File(basedir, "target/test-reports/test/TEST-test-with-resolve-outputdir-0.0.1.xml")
assert testsuite_xml.isFile();
testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.test-with-resolve-outputdir'
assert testsuite.@tests == 2
assert testsuite.@errors == 0
assert testsuite.@failures == 0
