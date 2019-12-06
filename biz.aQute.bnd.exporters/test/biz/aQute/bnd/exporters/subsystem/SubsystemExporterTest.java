package biz.aQute.bnd.exporters.subsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.After;
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

public class SubsystemExporterTest {
	private static final String	proj_bundle_1							= "proj_bundle_1";
	private static final String	proj_bundle_2							= "proj_bundle_2";

	private static final String	proj_bundle_1jar						= proj_bundle_1 + "-0.0.1.jar";
	private static final String	proj_bundle_2jar						= proj_bundle_2 + "-0.0.1.jar";

	private static final String	WS_PATH									= "test-ws/";

	private static final String	EXPORT_PRJ_FLD_SEG						= "proj_export";

	private static final String	bndRunSubSystemDefaults					= "SubSystemDefaults";
	private static final String	bndRunSubSystemOverride					= "SubSystemOverride";
	private static final String	bndRunSubSystemEsaArchiveTypeNone		= "SubSystemTypeNone";
	private static final String	bndRunSubSystemEsaArchiveTypeContent	= "SubSystemTypeContent";
	private static final String	bndRunSubSystemEsaArchiveTypeAll		= "SubSystemTypeAll";

	@Rule
	public final TestName		testName								= new TestName();
	private String				genWsPath;
	private Workspace			ws;

	@Before
	public void setUp() throws Exception {
		genWsPath = "generated/tmp/test/" + getClass().getName() + "/" + testName.getMethodName() + "/" + WS_PATH;
		File wsRoot = IO.getFile(genWsPath);
		IO.delete(wsRoot);
		IO.copy(IO.getFile(WS_PATH), wsRoot);
		ws = new Workspace(wsRoot);
		ws.addBasicPlugin(new SubsystemExporter());

		Project p1 = ws.getProject(proj_bundle_1);
		assertThat(p1).isNotNull();
		assertThat(p1.build()).hasSize(1);

		Project p2 = ws.getProject(proj_bundle_2);
		assertThat(p2).isNotNull();
		assertThat(p2.build()).hasSize(1);

		File fsubs = IO.getFile(genWsPath + "/proj_subsys/subsys_a.bndrun");
		assertThat(fsubs).exists();

		try (Run subsys = Run.createRun(ws, fsubs)) {
			Map.Entry<String, Resource> subsysEas = subsys.export(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, null);
			try (JarResource subsysjarResource = (JarResource) subsysEas.getValue()) {
				Jar subsysjar = subsysjarResource.getJar();

				File subsysoutput = IO.getFile(genWsPath + "/proj_subsys/");
				Files.createDirectories(subsysoutput.toPath());
				File subsysesa = new File(subsysoutput, subsysEas.getKey());
				subsysjar.write(subsysesa.getAbsolutePath());
			}
		}
	}

	@After
	public void tearDown() {
		ws.close();
	}

	@Test
	public void testSubSystemDefaults() throws Exception {
		File esa = createExportEntry(bndRunSubSystemDefaults, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(esa, bndRunSubSystemDefaults, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemOverride() throws Exception {
		File esa = createExportEntry(bndRunSubSystemOverride, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		check(esa, bndRunSubSystemOverride, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, "0.0.1",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemArchiveTypeAll() throws Exception {
		File esa = createExportEntry(bndRunSubSystemEsaArchiveTypeAll, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(esa, bndRunSubSystemEsaArchiveTypeAll, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar, proj_bundle_2jar));
	}

	@Test
	public void testSubSystemArchiveTypeContent() throws Exception {
		File esa = createExportEntry(bndRunSubSystemEsaArchiveTypeContent, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(esa, bndRunSubSystemEsaArchiveTypeContent, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList(proj_bundle_1jar));
	}

	@Test
	public void testSubSystemArchiveTypeNone() throws Exception {
		File esa = createExportEntry(bndRunSubSystemEsaArchiveTypeNone, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		check(esa, bndRunSubSystemEsaArchiveTypeNone, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, "0.0.0",
			Arrays.asList());
	}

	private void check(File esa, String ssn, String ssType, String version, List<String> embeddedFileNames)
		throws Exception {
		try (Jar jar = new Jar(esa)) {
			Resource subsystemMFResource = jar.getResource("OSGI-INF/SUBSYSTEM.MF");
			assertThat(subsystemMFResource).isNotNull();
			Manifest subsystemMF = new Manifest(subsystemMFResource.openInputStream());
			assertThat(subsystemMF.getMainAttributes()).containsEntry(new Attributes.Name("Manifest-Version"), "1.0")
				.containsEntry(new Attributes.Name(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME), ssn)
				.containsEntry(new Attributes.Name(SubsystemConstants.SUBSYSTEM_TYPE), ssType)
				.containsEntry(new Attributes.Name(SubsystemConstants.SUBSYSTEM_MANIFESTVERSION), "1")
				.containsEntry(new Attributes.Name(SubsystemConstants.SUBSYSTEM_VERSION), version);

			for (String filename : embeddedFileNames) {
				assertThat(jar.exists(filename)).isTrue();
			}
		}
	}

	private File createExportEntry(String bndrun, String exportType) throws Exception {
		File f = IO.getFile(genWsPath + EXPORT_PRJ_FLD_SEG + "/" + bndrun + ".bndrun");
		assertThat(f).isFile();

		try (Run run = Run.createRun(ws, f)) {
			assertThat(run).isNotNull();
			assertThat(run.getName()).isEqualTo(bndrun + ".bndrun");

			Map.Entry<String, Resource> export = run.export(exportType, null);
			assertThat(export).isNotNull();

			try (JarResource jarResource = (JarResource) export.getValue()) {
				Jar jar = jarResource.getJar();

				File output = IO.getFile(genWsPath + "/result");
				Files.createDirectories(output.toPath());
				File esa = new File(output, export.getKey());

				jar.write(esa);
				return esa;
			}
		}
	}
}
