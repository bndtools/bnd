package test.bndmodel;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
import aQute.bnd.osgi.*;
import aQute.bnd.properties.*;
import aQute.lib.io.*;

public class BndModelTest  extends TestCase {

	static final String BND_BUILDPATH = "-buildpath: \\\n" +
										"\torg.apache.felix.dependencymanager,\\\n" +
										"\tosgi.core\n";

	static final String BND_BUILDPATH_EXPECTED = "-buildpath:  \\\n" +
												 "\tosgi.core\n";

	public void testSetBuildPath() throws Exception {

		BndEditModel model = new BndEditModel();
		File bndFile = getFile(BND_BUILDPATH);

		model.loadFrom(bndFile);
		List<VersionedClause> buildPath = model.getBuildPath();

		//The remove causes the actual problem
		buildPath.remove(0);
		model.setBuildPath(buildPath);

		Document document = new Document(IO.collect(bndFile));
		model.saveChangesTo(document);

		String data = document.get();

		assertEquals(BND_BUILDPATH_EXPECTED, data);
	}

	
	/**
	 * Check if we can get a processor of the model and verify that
	 * we get the proper properties.
	 */
	public void testParent() throws Exception {
		
		BndEditModel model = new BndEditModel();
		model.setRunFw("${fw}"); // set changes
		File f = new File("testresources/bndmodel/test-01.bndrun");
		model.setBndResource(f);
		
		Processor p = model.getProperties();
		

		assertEquals("Set in file, refers to macro", "a, b, c", p.getProperty(Constants.RUNBUNDLES));
		assertEquals("Changes, refers to macro", "framework", p.getProperty(Constants.RUNFW));
		
	}
	
	
	
	private static File getFile(String data) throws IOException {
		File file = File.createTempFile("bndtest", "bnd");
		file.deleteOnExit();
		IO.store(data, file);
		return file;
	}
}
