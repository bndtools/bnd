package biz.aQute.bnd.exporter.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.exporter.template.TemplateExporter;
import aQute.bnd.osgi.Jar;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class TemplatedExporterTest {

	private static final String	WS_PATH	= "test-ws-templated-exporter/";

	@InjectTemporaryDirectory
	File				testDir;

	private Workspace			workspace;
	private Project				project;

	// @BeforeEach
	// public void setUp() throws Exception {
	// prior = new Properties();
	// prior.putAll(System.getProperties());
	//
	// File wsRoot = new File(testDir, "test ws");
	// for (String folder : Arrays.asList("cnf", "demo", "biz.aQute.launcher",
	// "biz.aQute.junit", "biz.aQute.tester",
	// "biz.aQute.tester.junit-platform")) {
	// File tgt = new File(wsRoot, folder);
	// IO.copy(new File("..", folder), tgt);
	// IO.delete(new File(tgt, "generated/buildfiles"));
	// }
	// IO.delete(new File(wsRoot, "cnf/cache"));
	// workspace = new Workspace(wsRoot);
	// workspace.setProperty(Constants.PROFILE, "prod");
	// workspace.setProperty(Constants.CONNECTION_SETTINGS, "false");
	// project = workspace.getProject("demo");
	// project.setTrace(true);
	// assertTrue(project.check());
	// }
	//
	// @SuppressWarnings("restriction")
	// @AfterEach
	// public void tearDown() throws Exception {
	// IO.close(project);
	// IO.close(workspace);
	// System.setProperties(prior);
	// }

	@BeforeEach
	public void setUp(@InjectTemporaryDirectory
	String tmp) throws Exception {
		File wsRoot = IO.getFile(testDir, "test_ws");
		IO.delete(wsRoot);
		IO.copy(IO.getFile(WS_PATH), wsRoot);
		workspace = new Workspace(wsRoot);
		workspace.addBasicPlugin(new TemplateExporter());
		workspace.getRepositories();
		assertThat(workspace.check()).isTrue();
		project = workspace.getProject("template_export");
		// project.setTrace(true);
		// assertTrue(project.check());
	}

	@AfterEach
	public void tearDown() {
		workspace.close();
	}

	@Test
	public void testExportWithTemplate() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "y.bndrun;type=bnd.templated;name=test.zip");
		IO.store("-include: z.bndrun\n" + "-exporttype: bnd.templated;" + "template=testresources/template.jar;"
			+ "runbundlesDir=runbundles",
			project.getFile("y.bndrun"));
		File file = project.getFile("generated/template_export.jar");
		file.delete();
		assertThat(file).doesNotExist();

		File[] build = project.build();
		assertThat(project.check()).isTrue();
		assertThat(build).hasSize(2);
		assertThat(build[0]).isEqualTo(file);

		File run = project.getFile("generated/test.zip");
		assertThat(build[1]).isEqualTo(run);
		try (Jar jar = new Jar(run)) {
			assertThat(jar.getResource("test/org.apache.felix.gogo.runtime-1.1.0.v20180713-1646.jar")).isNotNull();
			assertThat(jar.getResource("runbundles/org.apache.felix.gogo.runtime-1.1.0.jar"))
				.isNotNull();
		}
	}

	@Test
	public void testExportWithTemplateFrameworkDir() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "y.bndrun;type=bnd.templated;name=test.zip");
		IO.store("-include: z.bndrun\n" + "-exporttype: bnd.templated;" + "template=testresources/template.jar;"
			+ "runbundlesDir=runbundles;frameworkTarget=framework/", project.getFile("y.bndrun"));
		File file = project.getFile("generated/template_export.jar");
		file.delete();
		assertThat(file).doesNotExist();

		File[] build = project.build();
		assertThat(project.check()).isTrue();
		assertThat(build).hasSize(2);
		assertThat(build[0]).isEqualTo(file);

		File run = project.getFile("generated/test.zip");
		assertThat(build[1]).isEqualTo(run);
		try (Jar jar = new Jar(run)) {
			assertThat(jar.getResource("test/org.apache.felix.gogo.runtime-1.1.0.v20180713-1646.jar")).isNotNull();
			assertThat(jar.getResource("runbundles/org.apache.felix.gogo.runtime-1.1.0.jar"))
				.isNotNull();
			assertThat(jar.getResource("framework/org.apache.felix.framework-5.6.10.jar")).isNotNull();
		}
	}

	@Test
	public void testExportWithTemplateFramework() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "y.bndrun;type=bnd.templated;name=test.zip");
		IO.store("-include: z.bndrun\n" + "-exporttype: bnd.templated;" + "template=testresources/template.jar;"
			+ "runbundlesDir=runbundles;frameworkTarget=framework/felix.jar", project.getFile("y.bndrun"));
		File file = project.getFile("generated/template_export.jar");
		file.delete();
		assertThat(file).doesNotExist();

		File[] build = project.build();
		assertThat(project.check()).isTrue();
		assertThat(build).hasSize(2);
		assertThat(build[0]).isEqualTo(file);

		File run = project.getFile("generated/test.zip");
		assertThat(build[1]).isEqualTo(run);
		try (Jar jar = new Jar(run)) {
			assertThat(jar.getResource("test/org.apache.felix.gogo.runtime-1.1.0.v20180713-1646.jar")).isNotNull();
			assertThat(jar.getResource("runbundles/org.apache.felix.gogo.runtime-1.1.0.jar"))
				.isNotNull();
			assertThat(jar.getResource("framework/felix.jar")).isNotNull();
		}
	}

	@Test
	public void testExportIncluderesource() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "y.bndrun;type=bnd.templated;name=test.zip;bsn=test.bsn;version=1.0.0");
		IO.store("-include: z.bndrun\n" + "-includeresource: testresources/template.jar\n"
			+ "-exporttype: bnd.templated;" + "runbundlesDir=runbundles", project.getFile("y.bndrun"));
		File file = project.getFile("generated/template_export.jar");
		file.delete();
		assertThat(file).doesNotExist();

		File[] build = project.build();
		assertThat(project.check()).isTrue();
		assertThat(build).hasSize(2);
		assertThat(build[0]).isEqualTo(file);

		File run = project.getFile("generated/test.zip");
		assertThat(build[1]).isEqualTo(run);
		try (Jar jar = new Jar(run)) {
			assertThat(jar.getResource("runbundles/org.apache.felix.gogo.runtime-1.1.0.jar"))
				.isNotNull();
			assertThat(jar.getResource("template.jar")).isNotNull();
			assertThat(jar.getResources()).hasSize(4);
		}
	}

	@Test
	public void testExportMetaData() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "y.bndrun;type=bnd.templated;name=test.zip;bsn=test.bsn;version=1.0.0");
		IO.store(
			"-include: z.bndrun\n" + "Test: TestHeader\n" + "-exporttype: bnd.templated;"
				+ "runbundlesDir=runbundles;metadata=true",
			project.getFile("y.bndrun"));
		File file = project.getFile("generated/template_export.jar");
		file.delete();
		assertThat(file).doesNotExist();

		File[] build = project.build();
		assertThat(project.check()).isTrue();
		assertThat(build).hasSize(2);
		assertThat(build[0]).isEqualTo(file);

		File run = project.getFile("generated/test.zip");
		assertThat(build[1]).isEqualTo(run);
		try (Jar jar = new Jar(run)) {
			assertThat(jar.getResource("runbundles/org.apache.felix.gogo.runtime-1.1.0.jar"))
				.isNotNull();
			assertThat(jar.getResource("META-INF/MANIFEST.MF")).isNotNull();
		}
	}

	@Test
	public void testExportRunbundlesDirNotSetError() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "y.bndrun;type=bnd.templated;name=test.zip");
		IO.store("-include: z.bndrun\n" + "-exporttype: bnd.templated;", project.getFile("y.bndrun"));
		File file = project.getFile("generated/template_export.jar");
		file.delete();
		assertThat(file).doesNotExist();

		project.build();
		assertThat(project.check()).isFalse();
		assertThat(project.getErrors())
			.anyMatch("When -runbundles is present, the runbundlesDir Attribute must be set"::equals);
	}

	@Test
	public void testExportRunpathDirNotSetError() throws Exception {
		project.setProperty("-resourceonly", "true");
		project.setProperty("-includeresource", "hello;literal=true");

		project.setProperty("-export", "y.bndrun;type=bnd.templated;name=test.zip");
		IO.store("-include: z.bndrun\n" + "-runpath: org.apache.felix.gogo.shell\n" + "-exporttype: bnd.templated;",
			project.getFile("y.bndrun"));
		File file = project.getFile("generated/template_export.jar");
		file.delete();
		assertThat(file).doesNotExist();

		project.build();
		assertThat(project.check()).isFalse();
		assertThat(project.getErrors())
			.anyMatch("When -runbundles is present, the runbundlesDir Attribute must be set"::equals);
		assertThat(project.getErrors())
			.anyMatch("When -runpath is present, the runpathDir Attribute must be set"::equals);
	}
}
