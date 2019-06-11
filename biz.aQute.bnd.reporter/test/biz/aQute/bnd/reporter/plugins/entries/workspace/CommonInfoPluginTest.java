package biz.aQute.bnd.reporter.plugins.entries.workspace;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import biz.aQute.bnd.reporter.manifest.dto.CommonInfoDTO;
import biz.aQute.bnd.reporter.plugins.entries.bndworkspace.CommonInfoPlugin;
import junit.framework.TestCase;

public class CommonInfoPluginTest extends TestCase {

	public void testNoPropAndHeader() throws Exception {
		final CommonInfoPlugin plugin = new CommonInfoPlugin();

		final CommonInfoDTO infoDto = plugin.extract(getWorkspace(), Locale.forLanguageTag("und"));

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
		final CommonInfoPlugin plugin = new CommonInfoPlugin();

		final CommonInfoDTO infoDto = plugin.extract(
			getWorkspace("ws-copyright", "test1", "ws-description", "test2", "ws-docURL", "test3", "ws-name", "test4",
				"ws-updateLocation", "test5", "ws-vendor", "test6", "ws-contactAddress", "test7", "ws-developers",
				"test8", "ws-icons", "test9", "ws-licenses", "test10", "ws-scm", "url=test11", "ws-version", "1.0.0"),
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
		final CommonInfoPlugin plugin = new CommonInfoPlugin();

		final CommonInfoDTO infoDto = plugin.extract(
			getWorkspace(Constants.BUNDLE_COPYRIGHT, "test1", Constants.BUNDLE_DESCRIPTION, "test2",
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
		final CommonInfoPlugin plugin = new CommonInfoPlugin();

		final CommonInfoDTO infoDto = plugin.extract(
			getWorkspace(Constants.BUNDLE_COPYRIGHT, "not", "ws-copyright", "test1", Constants.BUNDLE_DESCRIPTION,
				"test2", Constants.BUNDLE_DOCURL, "test3", Constants.BUNDLE_UPDATELOCATION, "test5",
				Constants.BUNDLE_VENDOR, "test6", "ws-vendor", "", Constants.BUNDLE_CONTACTADDRESS, "test7",
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

	private Workspace getWorkspace(final String... prop) throws Exception {
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

		final Iterator<String> it = Arrays.asList(prop)
			.iterator();
		while (it.hasNext()) {
			ws.set(it.next(), it.next());
		}

		return ws;
	}
}
