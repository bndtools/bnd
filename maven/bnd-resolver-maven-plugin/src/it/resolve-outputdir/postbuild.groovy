import aQute.bnd.build.Workspace
import aQute.bnd.build.model.BndEditModel
import aQute.bnd.osgi.Processor
import aQute.bnd.properties.Document
import aQute.lib.io.IO;

File bndrunFile = new File(basedir, 'bndruns/test.bndrun')
assert context.get("timestamp") == bndrunFile.lastModified()
assert context.get("length") == bndrunFile.length()

// Check the output bndrun file exist!
bndrunFile = new File(basedir, 'target/test.bndrun')
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
assert bemRunBundles.size() == 1

StringBuilder sb = new StringBuilder()
bemRunBundles.get(0).formatTo(sb)
assert sb.toString() == "org.apache.felix.eventadmin;version='[1.4.6,1.4.7)'"
