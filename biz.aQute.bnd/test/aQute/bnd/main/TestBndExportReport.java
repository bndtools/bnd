package aQute.bnd.main;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.Test;

import aQute.bnd.main.testrules.WatchedFolder.FileStatus;
import aQute.lib.io.IO;

public class TestBndExportReport extends TestBndMainBase {

	@Test
	public void testWorkspaceListNotExecuted() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd(Paths.get("p2"), "exportreport", "list");

		expectNoError();
		expectOutput("Project[p2]: no reports");
	}

	@Test
	public void testWorkspaceExportNotExecuted() throws Exception {
		initTestData(WORKSPACE);

		executeBndCmd(Paths.get("p2"), "exportreport", "export");

		expectNoError();
		expectOutput("Project[p2]: no reports");
	}

	@Test
	public void testListWhenEmpty() throws Exception {
		initTestData(WORKSPACE);

		executeList();

		expectNoError();
		expectOutputContains("Workspace[" + folder.getRootPath()
			.getFileName() + "]: no reports");
		expectOutputContains("Project[p]: no reports");
		expectOutputContains("Project[p2]: no reports");
		expectOutputContains("Project[p3]: no reports");
		expectOutputContains("Project[p4]: no reports");
	}

	@Test
	public void testListBasic() throws Exception {
		initTestData(WORKSPACE);

		setupExportInstruction("cnf/build.bnd", "metadata.xml");
		executeList();

		expectNoError();
		expectOutputContains("Workspace[" + folder.getRootPath()
			.getFileName() + "]: " + folder.getFile("metadata.xml"));
		expectOutputContains("Project[p]: " + folder.getFile("p/metadata.xml"));
		expectOutputContains("Project[p2]: " + folder.getFile("p2/metadata.xml"));
		expectOutputContains("Project[p3]: " + folder.getFile("p3/metadata.xml"));
		expectOutputContains("Project[p4]: " + folder.getFile("p4/metadata.xml"));
	}

	@Test
	public void testListScoped() throws Exception {
		initTestData(WORKSPACE);

		setupExportInstruction("cnf/build.bnd", "metadata.xml;scope=project");
		executeList();

		expectNoError();
		expectOutputContains("Workspace[" + folder.getRootPath()
			.getFileName() + "]: no reports");
		expectOutputContains("Project[p]: " + folder.getFile("p/metadata.xml"));
		expectOutputContains("Project[p2]: " + folder.getFile("p2/metadata.xml"));
		expectOutputContains("Project[p3]: " + folder.getFile("p3/metadata.xml"));
		expectOutputContains("Project[p4]: " + folder.getFile("p4/metadata.xml"));
	}

	@Test
	public void testExportWhenEmpty() throws Exception {
		initTestData(WORKSPACE);

		executeExport();

		expectNoError();
		expectOutputContains("Workspace[" + folder.getRootPath()
			.getFileName() + "]: no reports");
		expectOutputContains("Project[p]: no reports");
		expectOutputContains("Project[p2]: no reports");
		expectOutputContains("Project[p3]: no reports");
		expectOutputContains("Project[p4]: no reports");
	}

	@Test
	public void testExport() throws Exception {
		initTestData(WORKSPACE);

		setupExportInstruction("cnf/build.bnd", "metadata.xml");
		executeExport();

		expectNoError();
		expectOutputContains("Workspace[" + folder.getRootPath()
			.getFileName() + "]: " + folder.getFile("metadata.xml"));
		expectOutputContains("Project[p]: " + folder.getFile("p/metadata.xml"));
		expectOutputContains("Project[p2]: " + folder.getFile("p2/metadata.xml"));
		expectOutputContains("Project[p3]: " + folder.getFile("p3/metadata.xml"));
		expectOutputContains("Project[p4]: " + folder.getFile("p4/metadata.xml"));

		expectFileStatus(FileStatus.CREATED, "metadata.xml");
		expectFileStatus(FileStatus.CREATED, "p", "metadata.xml");
		expectFileStatus(FileStatus.CREATED, "p2", "metadata.xml");
		expectFileStatus(FileStatus.CREATED, "p3", "metadata.xml");
		expectFileStatus(FileStatus.CREATED, "p4", "metadata.xml");
	}

	@Test
	public void testReadmeExport() throws Exception {
		initTestData(WORKSPACE);

		executeReadmeExport();

		expectNoError();
		expectOutputContains("Workspace[" + folder.getRootPath()
			.getFileName() + "]: " + folder.getFile("readme.md"));
		expectOutputContains("Project[p]: " + folder.getFile("p/readme.md"));
		expectOutputContains("Project[p2]: " + folder.getFile("p2/readme.md"));
		expectOutputContains("Project[p3]: " + folder.getFile("p3/readme.md"));
		expectOutputContains("Project[p4]: " + folder.getFile("p4/readme.md"));

		expectFileStatus(FileStatus.CREATED, "readme.md");
		expectFileStatus(FileStatus.CREATED, "p", "readme.md");
		expectFileStatus(FileStatus.CREATED, "p2", "readme.md");
		expectFileStatus(FileStatus.CREATED, "p3", "readme.md");
		expectFileStatus(FileStatus.CREATED, "p4", "readme.md");
	}

	@Test
	public void testExportScoped() throws Exception {
		initTestData(WORKSPACE);

		setupExportInstruction("cnf/build.bnd", "metadata.xml;scope=project");

		executeExport("--project", "p2");

		expectNoError();
		expectOutput("Project[p2]: " + folder.getFile("p2/metadata.xml"));
		expectFileStatus(FileStatus.CREATED, "p2", "metadata.xml");
	}

	@Test
	public void testJarExportBasic() throws Exception {
		initTestData(BUNDLES);

		executeBndCmd("exportreport", "jarexport", folder.getFile("com.liferay.item.selector.taglib.jar")
			.toString(),
			folder.getRootPath()
				.toString() + "/metadata.xml");

		expectNoError();
		expectOutput("Jar[com.liferay.item.selector.taglib]: " + folder.getFile("metadata.xml")
			.toString());
		expectFileStatus(FileStatus.CREATED, "metadata.xml");
	}

	@Test
	public void testJarExportWithOption() throws Exception {
		initTestData(BUNDLES);

		String prop = "-reportconfig.test: anyEntry;key=asterix;value=obelix";
		String template = "{{ param }},{{ report.asterix }},{{ report.manifest.bundleSymbolicName.symbolicName }}";

		IO.write(prop.getBytes(), Paths.get(folder.getRootPath()
			.toString(), "prop.prop")
			.toFile());
		IO.write(template.getBytes(), Paths.get(folder.getRootPath()
			.toString(), "template")
			.toFile());

		executeBndCmd("exportreport", "jarexport", "--properties", folder.getFile("prop.prop")
			.toString(), "--configName", "test", "--templateType", "twig", "--template",
			folder.getFile("template")
				.toString(),
			"--parameters", "param=value", folder.getFile("com.liferay.item.selector.taglib.jar")
				.toString(),
			folder.getRootPath()
				.toString() + "/result.txt");

		expectNoError();
		expectOutput("Jar[com.liferay.item.selector.taglib]: " + folder.getFile("result.txt")
			.toString());
		expectFileStatus(FileStatus.CREATED, "result.txt");

		assertEquals(new String("value,obelix,com.liferay.item.selector.taglib"),
			new String(IO.read(folder.getFile("result.txt"))));
	}

	@Test
	public void testJarReadmeExport() throws Exception {
		initTestData(BUNDLES);

		executeBndCmd("exportreport", "jarreadme", folder.getFile("com.liferay.item.selector.taglib.jar")
			.toString(),
			folder.getRootPath()
				.toString() + "/readme.md");

		expectNoError();
		expectOutput("Jar[com.liferay.item.selector.taglib]: " + folder.getFile("readme.md")
			.toString());
		expectFileStatus(FileStatus.CREATED, "readme.md");
	}

	public void setupExportInstruction(String path, String instruction) throws Exception {
		Properties prop = new Properties();
		prop.load(Files.newInputStream(folder.getFile(path)
			.toPath()));
		prop.put("-exportreport", instruction);
		prop.store(Files.newOutputStream(folder.getFile(path)
			.toPath()), null);
	}

	public void executeList(String... args) throws Exception {
		execute("list", args);
	}

	public void executeExport(String... args) throws Exception {
		execute("export", args);
	}

	public void executeReadmeExport(String... args) throws Exception {
		execute("readme", args);
	}

	public void execute(String subCmd, String... args) throws Exception {
		args = args == null ? new String[0] : args;
		String[] line = new String[args.length + 4];
		line[0] = "exportreport";
		line[1] = subCmd;
		line[2] = "--workspace";
		line[3] = folder.getRootPath()
			.toString();

		System.arraycopy(args, 0, line, 4, args.length);

		executeBndCmd(line);
	}
}
