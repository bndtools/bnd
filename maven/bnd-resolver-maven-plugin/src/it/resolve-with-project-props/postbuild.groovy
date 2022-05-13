import aQute.bnd.build.*;
import aQute.bnd.build.model.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;

// The resolve-infer-with-custom-bsn case

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
assert bemRunBundles.size() == 2

StringBuilder sb = new StringBuilder()
bemRunBundles.get(0).formatTo(sb)
assert sb.toString() == "org.apache.felix.eventadmin;version='[1.4.8,1.4.9)'"
sb = new StringBuilder()
bemRunBundles.get(1).formatTo(sb)
assert sb.toString() == "resolve-with-project-props;version='[0.0.1,0.0.2)'"
