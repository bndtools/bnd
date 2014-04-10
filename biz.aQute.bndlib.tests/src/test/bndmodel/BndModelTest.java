package test.bndmodel;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.model.*;
import aQute.bnd.build.model.clauses.*;
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

	private static File getFile(String data) throws IOException {
		File file = File.createTempFile("bndtest", "bnd");
		file.deleteOnExit();
		IO.store(data, file);
		return file;
	}
}
