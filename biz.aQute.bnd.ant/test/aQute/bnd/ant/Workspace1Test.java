package aQute.bnd.ant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import aQute.lib.io.IO;

public class Workspace1Test {
	@Rule
	public final BuildFileRule	buildRule	= new BuildFileRule();
	public static final String	TMPDIR		= "generated/tmp/test";
	@Rule
	public final TestName		testName	= new TestName();
	private File				testDir;

	@Before
	public void setUp() throws Exception {
		testDir = new File(TMPDIR, getClass().getName() + "/" + testName.getMethodName());
		IO.delete(testDir);
		IO.mkdirs(testDir);
		IO.copy(IO.getFile(IO.getFile("testresources"), getClass().getSimpleName()), testDir);
		buildRule.configureProject(IO.getFile(testDir, "test.simple/build.xml")
			.getAbsolutePath());
	}

	@Test
	public void prepare_task() {
		buildRule.executeTarget("init");
		Project project = buildRule.getProject();
		Map<String, Object> properties = project.getProperties();
		assertThat(properties).containsEntry("project.name", "test.simple")
			.containsEntry("environment", "ant")
			.containsEntry("in.ant", "true")
			.containsEntry("p", "test.simple")
			.containsEntry("Foo", "foo")
			.containsEntry("Bar", "bar")
			.containsEntry("Test-Cases", "test.simple.Test");
	}

	@Test
	public void build_task() throws Exception {
		buildRule.executeTarget("build");
		Project project = buildRule.getProject();
		File generated = IO.getFile(project.getProperty("target"));
		assertThat(generated).isDirectory();
		File simple_bundle = IO.getFile(generated, "test.simple.jar");
		assertThat(simple_bundle).isFile();
		try (JarFile simple_jar = new JarFile(simple_bundle)) {
			Attributes simple_manifest = simple_jar.getManifest()
				.getMainAttributes();
			assertThat(simple_manifest).containsEntry(new Attributes.Name("Bundle-SymbolicName"), "test.simple")
				.hasEntrySatisfying(new Attributes.Name("Bundle-Version"), value -> value.toString()
					.startsWith("0.0.0."))
				.containsEntry(new Attributes.Name("Foo"), "foo")
				.containsEntry(new Attributes.Name("Bar"), "bar")
				.hasEntrySatisfying(new Attributes.Name("Import-Package"), value -> value.toString()
					.contains("junit.framework"));
			assertThat(simple_jar.getEntry("test/simple/Simple.class")).isNotNull();
			assertThat(simple_jar.getEntry("test/simple/Test.class")).isNotNull();
			assertThat(simple_jar.getEntry("OSGI-OPT/src/")).isNotNull();
			assertThat(simple_jar.getEntry("test.txt")).isNotNull();
			assertThat(simple_jar.getInputStream(simple_jar.getEntry("test.txt")))
				.hasContent("This is a project resource");
			assertThat(simple_jar.getEntry("test/simple/test.txt")).isNotNull();
			assertThat(simple_jar.getInputStream(simple_jar.getEntry("test/simple/test.txt")))
				.hasContent("This is a package resource");
		}
	}

	@Test
	public void release_task() throws Exception {
		buildRule.executeTarget("release");
		Project project = buildRule.getProject();
		File build = IO.getFile(project.getProperty("build"));
		assertThat(build).isDirectory();
		File simple_bundle = IO.getFile(build, "releaserepo/test.simple/test.simple-0.0.0.jar");
		assertThat(simple_bundle).isFile();
	}

	@Test
	public void test_task() throws Exception {
		buildRule.executeTarget("test");
		Project project = buildRule.getProject();
		File generated = IO.getFile(project.getProperty("target"));
		assertThat(generated).isDirectory();
		File testReports = IO.getFile(generated, "test-reports");
		assertThat(testReports).isDirectory();
		assertThat(IO.getFile(testReports, "TEST-test.simple-0.0.0.xml")).isFile();
	}

}
