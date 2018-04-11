package biz.aQute.bnd.exporters.subsystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.subsystem.SubsystemConstants;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.exporter.subsystem.SubsystemExporter;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class SubsystemTest extends TestCase {
	private String	proj_bundle_1		= "proj_bundle_1";
	private String	proj_bundle_2		= "proj_bundle_2";

	private String	proj_bundle_1jar	= proj_bundle_1 + "-0.0.1.jar";
	private String	proj_bundle_2jar	= proj_bundle_2 + "-0.0.1.jar";

	@BeforeClass
	protected void beforeClass() throws Throwable {

		File result = IO.getFile(RESULTS_PATH);

		Files.walk(result.toPath())
			.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);

	}

	private static final String	RESULTS_PATH						= "results/";
	private static final String	WS_PATH								= "test-ws/";
	private static final String	EXPORT_PRJ_FLD_SEG					= "proj_export";
	private static final String	EXPORT_PATH							= WS_PATH + EXPORT_PRJ_FLD_SEG + "/";
	private static final String	bndRunSubSystemDefaults				= "SubSystemDefaults.bndrun";
	private static final String	bndRunSubSystemOverride				= "SubSystemOverride.bndrun";

	private static final String	bndRunSubSystemEasArchivTypeNone	= "SubSystemTypeNone.bndrun";
	private static final String	bndRunSubSystemEasArchivTypeContent	= "SubSystemTypeContent.bndrun";
	private static final String	bndRunSubSystemEasArchivTypeAll		= "SubSystemTypeAll.bndrun";

	@Test
	public void testSubSystemDefaults() throws Exception {
		Jar jar = createExportEntry(bndRunSubSystemDefaults, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, EXPORT_PRJ_FLD_SEG, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemOverride() throws Exception {

		Jar jar = createExportEntry(bndRunSubSystemOverride, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		check(jar, EXPORT_PRJ_FLD_SEG, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, "0.0.1",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemArchiveTypeAll() throws Exception {
		Jar jar = createExportEntry(bndRunSubSystemEasArchivTypeAll, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, EXPORT_PRJ_FLD_SEG, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar, proj_bundle_2jar));
	}

	@Test
	public void testSubSystemArchiveTypeContent() throws Exception {
		Jar jar = createExportEntry(bndRunSubSystemEasArchivTypeContent, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, EXPORT_PRJ_FLD_SEG, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemArchiveTypeNone() throws Exception {
		Jar jar = createExportEntry(bndRunSubSystemEasArchivTypeNone, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(jar, EXPORT_PRJ_FLD_SEG, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0", Arrays.asList());
	}

	private void check(Jar jar, String ssn, String ssType, String version, List<String> embeddesFileNames)
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
		// assertEquals(jar.getResources()
		// .size(), embeddesFileNames.size() + subsystemFilesMF);

		for (String filename : embeddesFileNames) {
			assertNotNull(jar.getResource(filename));
		}

	}

	private Jar createExportEntry(String bndrun, String exportType) throws Exception {
		Workspace ws = getWS();

		Project p1 = new Project(ws, new File(WS_PATH + "/" + proj_bundle_1));

		p1.close();

		Project p2 = new Project(ws, new File(WS_PATH + "/" + proj_bundle_2));

		p2.close();

		Run subsys = Run.createRun(ws, new File(WS_PATH + "/proj_subsys/subsys.bndrun"));
		Map.Entry<String, Resource> subsysEas = subsys.export(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, null);
		JarResource subsysjarResource = (JarResource) subsysEas.getValue();

		Jar subsysjar = subsysjarResource.getJar();

		File subsysoutput = IO.getFile(WS_PATH + "/proj_subsys/");
		Files.createDirectories(subsysoutput.toPath());
		File subsysesa = new File(subsysoutput, subsysEas.getKey());
		subsysjar.write(subsysesa.getAbsolutePath());

		File f = IO.getFile(EXPORT_PATH + bndrun);

		assertNotNull(f);
		assertTrue(f.isFile());

		Run run = Run.createRun(ws, f);

		assertNotNull(run);
		assertEquals(run.getName(), bndrun);

		Map.Entry<String, Resource> export = run.export(exportType, null);

		assertNotNull(export);

		JarResource jarResource = (JarResource) export.getValue();

		Jar jar = jarResource.getJar();

		File output = IO.getFile(RESULTS_PATH + bndrun);
		Files.createDirectories(output.toPath());
		File esa = new File(output, export.getKey());

		jar.write(esa.getAbsolutePath());
		return jar;
	}

	private Workspace getWS() throws Exception {
		Workspace ws = Workspace.getWorkspace(new File(WS_PATH).getAbsoluteFile());

		ws.addBasicPlugin(new SubsystemExporter());
		return ws;
	}

}
