package biz.aQute.bnd.exporters.subsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.service.subsystem.SubsystemConstants;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.exporter.subsystem.SubsystemExporter;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

public class SubsystemTest {
	private static final String	proj_bundle_1						= "proj_bundle_1";
	private static final String	proj_bundle_2						= "proj_bundle_2";

	private static final String	proj_bundle_1jar					= proj_bundle_1 + "-0.0.1.jar";
	private static final String	proj_bundle_2jar					= proj_bundle_2 + "-0.0.1.jar";

	private static final String	WS_PATH								= "test-ws/";

	private static final String	EXPORT_PRJ_FLD_SEG					= "proj_export";

	private static final String	bndRunSubSystemDefaults				= "SubSystemDefaults";
	private static final String	bndRunSubSystemOverride				= "SubSystemOverride";
	private static final String	bndRunSubSystemEasArchivTypeNone	= "SubSystemTypeNone";
	private static final String	bndRunSubSystemEasArchivTypeContent	= "SubSystemTypeContent";
	private static final String	bndRunSubSystemEasArchivTypeAll		= "SubSystemTypeAll";

	@Rule
	public TestName				testName							= new TestName();

	private String getGenWsPath() {
		return "generated/tmp/test/" + testName.getMethodName() + "/" + WS_PATH;
	}

	@Before
	public void setUp() throws Exception {

		String genWsPath = getGenWsPath();

		IO.copy(new File(WS_PATH), new File(genWsPath));
		Workspace ws = getWS();

		Project p1 = new Project(ws, new File(genWsPath + "/" + proj_bundle_1));
		File[] filesp1 = p1.build();
		assertNotNull(filesp1);
		assertEquals(1, filesp1.length);
		p1.close();

		Project p2 = new Project(ws, new File(genWsPath + "/" + proj_bundle_2));
		File[] filesp2 = p2.build();
		assertNotNull(filesp2);
		assertEquals(1, filesp2.length);
		p2.close();

		File fsubs = new File(genWsPath + "/proj_subsys/subsys_a.bndrun");

		assertTrue(fsubs.exists());

		Run subsys = Run.createRun(ws, fsubs);
		Map.Entry<String, Resource> subsysEas = subsys.export(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, null);
		JarResource subsysjarResource = (JarResource) subsysEas.getValue();

		Jar subsysjar = subsysjarResource.getJar();

		File subsysoutput = IO.getFile(genWsPath + "/proj_subsys/");
		Files.createDirectories(subsysoutput.toPath());
		File subsysesa = new File(subsysoutput, subsysEas.getKey());
		subsysjar.write(subsysesa.getAbsolutePath());

	}

	@Test
	public void testSubSystemDefaults() throws Exception {

		Jar jar = createExportEntry(bndRunSubSystemDefaults, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, bndRunSubSystemDefaults, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemOverride() throws Exception {

		Jar jar = createExportEntry(bndRunSubSystemOverride, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		check(jar, bndRunSubSystemOverride, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, "0.0.1",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemArchiveTypeAll() throws Exception {
		Jar jar = createExportEntry(bndRunSubSystemEasArchivTypeAll, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, bndRunSubSystemEasArchivTypeAll, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar, proj_bundle_2jar));
	}

	@Test
	public void testSubSystemArchiveTypeContent() throws Exception {
		Jar jar = createExportEntry(bndRunSubSystemEasArchivTypeContent, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, bndRunSubSystemEasArchivTypeContent, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemArchiveTypeNone() throws Exception {
		Jar jar = createExportEntry(bndRunSubSystemEasArchivTypeNone, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, bndRunSubSystemEasArchivTypeNone, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList());
	}

	private void check(Jar jar, String ssn, String ssType, String version, List<String> embeddedFileNames)
		throws Exception {
		Resource subsystemMFResource = jar.getResource("OSGI-INF/SUBSYSTEM.MF");
		String subsystemMF = new String(subsystemMFResource.buffer()
			.array());

		assertTrue(subsystemMF.contains("Manifest-Version: 1.0"));
		assertTrue(subsystemMF.contains(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME + ": " + ssn));
		assertTrue(subsystemMF.contains(SubsystemConstants.SUBSYSTEM_TYPE + ": " + ssType));
		assertTrue(subsystemMF.contains(SubsystemConstants.SUBSYSTEM_MANIFESTVERSION + ": 1"));
		assertTrue(subsystemMF.contains(SubsystemConstants.SUBSYSTEM_VERSION + ": " + version));

		int subsystemFilesMF = 1;

		for (String filename : embeddedFileNames) {
			assertNotNull(jar.getResource(filename));
		}

	}

	private Jar createExportEntry(String bndrun, String exportType) throws Exception {
		Workspace ws = getWS();

		File f = IO.getFile(getGenWsPath() + EXPORT_PRJ_FLD_SEG + "/" + bndrun + ".bndrun");

		assertNotNull(f);
		assertTrue(f.isFile());

		Run run = Run.createRun(ws, f);

		assertNotNull(run);
		assertEquals(run.getName(), bndrun + ".bndrun");

		Map.Entry<String, Resource> export = run.export(exportType, null);

		assertNotNull(export);

		JarResource jarResource = (JarResource) export.getValue();

		Jar jar = jarResource.getJar();

		File output = IO.getFile(getGenWsPath() + "/result");
		Files.createDirectories(output.toPath());
		File esa = new File(output, export.getKey());

		jar.write(esa.getAbsolutePath());
		return jar;
	}

	private Workspace getWS() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File(getGenWsPath()).getAbsoluteFile());
		ws.addBasicPlugin(new SubsystemExporter());
		return ws;
	}
}