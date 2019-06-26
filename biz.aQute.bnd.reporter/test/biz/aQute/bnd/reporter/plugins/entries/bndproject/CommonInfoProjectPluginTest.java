package biz.aQute.bnd.reporter.plugins.entries.bndproject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import biz.aQute.bnd.reporter.manifest.dto.CommonInfoDTO;
import junit.framework.TestCase;

public class CommonInfoProjectPluginTest extends TestCase {

	public void testNoPropAndHeader() throws Exception {
		final CommonInfoProjectPlugin plugin = new CommonInfoProjectPlugin();

		final CommonInfoDTO infoDto = plugin.extract(getProject(), Locale.forLanguageTag("und"));

		assertNull(infoDto.copyright);
		assertNull(infoDto.description);
		assertNull(infoDto.docURL);
		assertNull(infoDto.name);
		assertNull(infoDto.updateLocation);
		assertNull(infoDto.vendor);
		assertNull(infoDto.contactAddress);
		assertNull(infoDto.developers);
		assertNull(infoDto.icons);
		assertNull(infoDto.licenses);
		assertNull(infoDto.scm);
		assertNull(infoDto.version);
	}

	public void testFullProp() throws Exception {
		final CommonInfoProjectPlugin plugin = new CommonInfoProjectPlugin();

		final CommonInfoDTO infoDto = plugin.extract(
			getProject("p-copyright", "test1", "p-description", "test2", "p-docURL", "test3", "p-name", "test4",
				"p-updateLocation", "test5", "p-vendor", "test6", "p-contactAddress", "test7", "p-developers", "test8",
				"p-icons", "test9", "p-licenses", "test10", "p-scm", "url=test11", "p-version", "1.0.0"),
			Locale.forLanguageTag("und"));

		assertEquals("test1", infoDto.copyright);
		assertEquals("test2", infoDto.description);
		assertEquals("test3", infoDto.docURL);
		assertEquals("test4", infoDto.name);
		assertEquals("test5", infoDto.updateLocation);
		assertEquals("test6", infoDto.vendor);
		assertEquals("test7", infoDto.contactAddress.address);
		assertEquals("postal", infoDto.contactAddress.type);
		assertEquals("test8", infoDto.developers.iterator()
			.next().identifier);
		assertEquals("test9", infoDto.icons.iterator()
			.next().url);
		assertEquals("test10", infoDto.licenses.iterator()
			.next().name);
		assertEquals("test11", infoDto.scm.url);
		assertEquals(1, infoDto.version.major);
	}

	public void testFullHeader() throws Exception {
		final CommonInfoProjectPlugin plugin = new CommonInfoProjectPlugin();

		final CommonInfoDTO infoDto = plugin.extract(
			getProject(Constants.BUNDLE_COPYRIGHT, "test1", Constants.BUNDLE_DESCRIPTION, "test2",
				Constants.BUNDLE_DOCURL, "test3", Constants.BUNDLE_NAME, "test4", Constants.BUNDLE_UPDATELOCATION,
				"test5", Constants.BUNDLE_VENDOR, "test6", Constants.BUNDLE_CONTACTADDRESS, "test7",
				Constants.BUNDLE_DEVELOPERS, "test8", Constants.BUNDLE_ICON, "test9", Constants.BUNDLE_LICENSE,
				"test10", Constants.BUNDLE_SCM, "url=test11", Constants.BUNDLE_VERSION, "1.0.0"),
			Locale.forLanguageTag("und"));

		assertEquals("test1", infoDto.copyright);
		assertEquals("test2", infoDto.description);
		assertEquals("test3", infoDto.docURL);
		assertEquals("test4", infoDto.name);
		assertEquals("test5", infoDto.updateLocation);
		assertEquals("test6", infoDto.vendor);
		assertEquals("test7", infoDto.contactAddress.address);
		assertEquals("postal", infoDto.contactAddress.type);
		assertEquals("test8", infoDto.developers.iterator()
			.next().identifier);
		assertEquals("test9", infoDto.icons.iterator()
			.next().url);
		assertEquals("test10", infoDto.licenses.iterator()
			.next().name);
		assertEquals("test11", infoDto.scm.url);
		assertEquals(1, infoDto.version.major);
	}

	public void testMixPropHeader() throws Exception {
		final CommonInfoProjectPlugin plugin = new CommonInfoProjectPlugin();

		final CommonInfoDTO infoDto = plugin.extract(
			getProject(Constants.BUNDLE_COPYRIGHT, "not", "p-copyright", "test1", Constants.BUNDLE_DESCRIPTION,
				"test2", Constants.BUNDLE_DOCURL, "test3", Constants.BUNDLE_UPDATELOCATION, "test5",
				Constants.BUNDLE_VENDOR, "test6", "p-vendor", "", Constants.BUNDLE_CONTACTADDRESS, "test7",
				Constants.BUNDLE_DEVELOPERS, "test8", Constants.BUNDLE_ICON, "test9", Constants.BUNDLE_LICENSE,
				"test10", Constants.BUNDLE_SCM, "url=test11", Constants.BUNDLE_VERSION, "1.0.0"),
			Locale.forLanguageTag("und"));

		assertEquals("test1", infoDto.copyright);
		assertEquals("test2", infoDto.description);
		assertEquals("test3", infoDto.docURL);
		assertNull(infoDto.name);
		assertEquals("test5", infoDto.updateLocation);
		assertNull(infoDto.vendor);
		assertEquals("test7", infoDto.contactAddress.address);
		assertEquals("postal", infoDto.contactAddress.type);
		assertEquals("test8", infoDto.developers.iterator()
			.next().identifier);
		assertEquals("test9", infoDto.icons.iterator()
			.next().url);
		assertEquals("test10", infoDto.licenses.iterator()
			.next().name);
		assertEquals("test11", infoDto.scm.url);
		assertEquals(1, infoDto.version.major);
	}

	private Workspace getWorkspace() throws Exception {
		final File wsFile = Files.createTempDirectory("bnd-ws")
			.toFile();
		wsFile.deleteOnExit();

		final File cnf = Files.createDirectory(Paths.get(wsFile.getPath(), "cnf"))
			.toFile();
		cnf.deleteOnExit();

		final File build = new File(cnf, "build.bnd");
		build.createNewFile();
		build.deleteOnExit();

		final Workspace ws = new Workspace(wsFile);

		return ws;
	}

	private Project getBareProject() throws Exception {
		final Workspace ws = getWorkspace();

		final File p1 = Files.createDirectory(Paths.get(ws.getBase()
			.getPath(), "project"))
			.toFile();
		p1.deleteOnExit();

		final File bnd1 = new File(p1, "bnd.bnd");
		bnd1.createNewFile();
		bnd1.deleteOnExit();

		return ws.getProject("project");
	}

	private Project getProject(final String... prop) throws Exception {
		final Project p = getBareProject();

		final Iterator<String> it = Arrays.asList(prop)
			.iterator();
		while (it.hasNext()) {
			p.set(it.next(), it.next());
		}

		return p;
	}
}
