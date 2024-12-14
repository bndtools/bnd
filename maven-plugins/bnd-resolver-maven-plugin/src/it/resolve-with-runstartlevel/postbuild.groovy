import aQute.bnd.build.Workspace
import aQute.bnd.build.model.BndEditModel
import aQute.bnd.osgi.Processor
import aQute.bnd.properties.Document
import aQute.lib.io.IO;

// The resolve-with-starlevel case

// Check the bndrun file exist!
File bndrunFile = new File(basedir, 'test.bndrun')
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
assert bemRunBundles.size() == 4

StringBuilder sb = new StringBuilder()
bemRunBundles.get(0).formatTo(sb)
assert sb.toString() == "org.apache.felix.eventadmin;version='[1.4.8,1.4.9)';startlevel=1000"

sb = new StringBuilder()
bemRunBundles.get(1).formatTo(sb)
assert sb.toString() == "ch.qos.logback.core;version='[1.2.13,1.2.14)';startlevel=1001"

sb = new StringBuilder()
bemRunBundles.get(2).formatTo(sb)
assert sb.toString() == "ch.qos.logback.classic;version='[1.2.13,1.2.14)';startlevel=1002"

sb = new StringBuilder()
bemRunBundles.get(3).formatTo(sb)
assert sb.toString() == "slf4j.api;version='[1.7.36,1.7.37)';startlevel=1003"
