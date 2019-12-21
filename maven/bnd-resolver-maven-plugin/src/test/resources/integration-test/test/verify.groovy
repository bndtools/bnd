import aQute.bnd.build.*;
import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;
import java.util.jar.*;

println "basedir ${basedir}"

// Check the bndrun file exist!
File bndrunFile = new File(basedir, 'resolve/test.bndrun')
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

// The resolve-from-defaults case

// Check the bndrun file exist!
bndrunFile = new File(basedir, 'resolve-from-defaults/test.bndrun')
assert bndrunFile.isFile()

// Load the BndEditModel of the bndrun file so we can inspect the result
processor = new Processor()
processor.setProperties(bndrunFile)
bem = new BndEditModel(Workspace.createStandaloneWorkspace(processor, bndrunFile.toURI()))
doc = new Document(IO.collect(bndrunFile))
bem.loadFrom(doc)

// Get the -runbundles.
bemRunBundles = bem.getRunBundles()
assert bemRunBundles
assert bemRunBundles.size() == 1

sb = new StringBuilder()
bemRunBundles.get(0).formatTo(sb)
assert sb.toString() == "resolve-from-defaults;version='[0.0.1,0.0.2)'"

// The resolve-from-dependencies case

// Check the bndrun file exist!
bndrunFile = new File(basedir, 'resolve-from-dependencies/test.bndrun')
assert bndrunFile.isFile()

// Load the BndEditModel of the bndrun file so we can inspect the result
processor = new Processor()
processor.setProperties(bndrunFile)
bem = new BndEditModel(Workspace.createStandaloneWorkspace(processor, bndrunFile.toURI()))
doc = new Document(IO.collect(bndrunFile))
bem.loadFrom(doc)

// Get the -runbundles.
bemRunBundles = bem.getRunBundles()
assert bemRunBundles
assert bemRunBundles.size() == 1

sb = new StringBuilder()
bemRunBundles.get(0).formatTo(sb)
assert sb.toString() == "org.apache.felix.eventadmin;version='[1.4.8,1.4.9)'"

// Check the bndrun file exist!
bndrunFile = new File(basedir, 'resolve-from-dependencyManagement/test.bndrun')
assert bndrunFile.isFile()

// Load the BndEditModel of the bndrun file so we can inspect the result
processor = new Processor()
processor.setProperties(bndrunFile)
bem = new BndEditModel(Workspace.createStandaloneWorkspace(processor, bndrunFile.toURI()))
doc = new Document(IO.collect(bndrunFile))
bem.loadFrom(doc)

// Get the -runbundles.
bemRunBundles = bem.getRunBundles()
assert bemRunBundles
assert bemRunBundles.size() == 1

sb = new StringBuilder()
bemRunBundles.get(0).formatTo(sb)
assert sb.toString() == "org.apache.felix.eventadmin;version='[1.4.8,1.4.9)'"

// The resolve-from-inputbundles case

// Check the bndrun file exist!
bndrunFile = new File(basedir, 'resolve-from-inputbundles/test.bndrun')
assert bndrunFile.isFile()

// Load the BndEditModel of the bndrun file so we can inspect the result
processor = new Processor()
processor.setProperties(bndrunFile)
bem = new BndEditModel(Workspace.createStandaloneWorkspace(processor, bndrunFile.toURI()))
doc = new Document(IO.collect(bndrunFile))
bem.loadFrom(doc)

// Get the -runbundles.
bemRunBundles = bem.getRunBundles()
assert bemRunBundles
assert bemRunBundles.size() == 1

sb = new StringBuilder()
bemRunBundles.get(0).formatTo(sb)
assert sb.toString() == "org.apache.felix.eventadmin;version='[1.4.8,1.4.9)'"


File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// No previous
assert !(build_log =~ java.util.regex.Pattern.quote("Resolution Result:\n[INFO]  org.apache.felix.eventadmin;version='[1.5.0,1.5.1)'"))
