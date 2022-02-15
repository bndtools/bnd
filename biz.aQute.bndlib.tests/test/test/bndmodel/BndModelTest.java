package test.bndmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.properties.Document;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class BndModelTest {

	static final String	BND_BUILDPATH			= "-buildpath: \\\n" + "\torg.apache.felix.dependencymanager,\\\n"
		+ "\tosgi.core\n";

	static final String	BND_BUILDPATH_EXPECTED	= "-buildpath: osgi.core\n";

	/**
	 * Test escaping of backslashes.
	 *
	 * @throws Exception
	 */

	@Test
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

	@Test
	public void testBadInput() throws UnsupportedEncodingException, IOException {
		BndEditModel model = new BndEditModel();

		model.loadFrom(new ByteArrayInputStream("-runproperties: bad=really=bad".getBytes("UTF-8")));

		Map<String, String> runProperties = model.getRunProperties();

		assertNotNull(runProperties);
		assertTrue(runProperties.containsKey("ERROR"));
	}

	@Test
	public void testSetBuildPath(@InjectTemporaryDirectory
	File tmp) throws Exception {

		BndEditModel model = new BndEditModel();
		File bndFile = File.createTempFile("bndtest", "bnd", tmp);
		IO.store(BND_BUILDPATH, bndFile);

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
	@Test
	public void testParent() throws Exception {

		BndEditModel model = new BndEditModel();
		model.setRunFw("${fw}"); // set changes
		File f = IO.getFile("testresources/bndmodel/test-01.bndrun");
		model.setBndResource(f);

		Processor p = model.getProperties();

		assertEquals("a, b, c", p.getProperty(Constants.RUNBUNDLES), "Set in file, refers to macro");
		assertEquals("framework", p.getProperty(Constants.RUNFW), "Changes, refers to macro");

	}
}
