package test.bndmodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.properties.Document;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class BndModelTest extends TestCase {

	static final String	BND_BUILDPATH			= "-buildpath: \\\n" + "\torg.apache.felix.dependencymanager,\\\n"
		+ "\tosgi.core\n";

	static final String	BND_BUILDPATH_EXPECTED	= "-buildpath: osgi.core\n";

	/**
	 * Test escaping of backslashes.
	 *
	 * @throws Exception
	 */

	public void testEscapingBackslashes() throws Exception {
		String longstring = "abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs tuv wxy z01 234 567 890";
		String formattedLongstring = "abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs \\\n"
			+ "	tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 \\\n"
			+ "	abc def ghi jkl mno pqrs tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs \\\n"
			+ "	tuv wxy z01 234 567 890 abc def ghi jkl mno pqrs tuv wxy z01 234 567 890";

		BndEditModel model = new BndEditModel();
		model.setBundleName("abc \\ def");
		model.setBundleDescription(longstring);
		model.setBundleActivator("\u1234 \n");

		assertEquals("abc \\ def", model.getBundleName());
		assertEquals("abc \\\\ def", model.getDocumentChanges()
			.get(Constants.BUNDLE_NAME));
		assertEquals("abc \\ def", model.getProperties()
			.get(Constants.BUNDLE_NAME));

		assertEquals(longstring, model.getBundleDescription());
		assertEquals(formattedLongstring, model.getDocumentChanges()
			.get(Constants.BUNDLE_DESCRIPTION));
		assertEquals(longstring, model.getProperties()
			.get(Constants.BUNDLE_DESCRIPTION));

		assertEquals("\u1234 \n", model.getBundleActivator());
		assertEquals("\u1234 \\n\\\n\t", model.getDocumentChanges()
			.get(Constants.BUNDLE_ACTIVATOR));
		assertEquals("\u1234 \n", model.getProperties()
			.get(Constants.BUNDLE_ACTIVATOR));

	}

	public void testBadInput() throws UnsupportedEncodingException, IOException {
		BndEditModel model = new BndEditModel();

		model.loadFrom(new ByteArrayInputStream("-runproperties: bad=really=bad".getBytes("UTF-8")));

		Map<String, String> runProperties = model.getRunProperties();

		assertNotNull(runProperties);
		assertTrue(runProperties.containsKey("ERROR"));
	}

	public void testSetBuildPath() throws Exception {

		BndEditModel model = new BndEditModel();
		File bndFile = getFile(BND_BUILDPATH);

		model.loadFrom(bndFile);
		List<VersionedClause> buildPath = model.getBuildPath();

		// The remove causes the actual problem
		buildPath.remove(0);
		model.setBuildPath(buildPath);

		Document document = new Document(IO.collect(bndFile));
		model.saveChangesTo(document);

		String data = document.get();

		assertEquals(BND_BUILDPATH_EXPECTED, data);
	}

	/**
	 * Check if we can get a processor of the model and verify that we get the
	 * proper properties.
	 */
	public void testParent() throws Exception {

		BndEditModel model = new BndEditModel();
		model.setRunFw("${fw}"); // set changes
		File f = IO.getFile("testresources/bndmodel/test-01.bndrun");
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
