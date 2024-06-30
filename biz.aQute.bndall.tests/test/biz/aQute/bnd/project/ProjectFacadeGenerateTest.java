package biz.aQute.bnd.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.FileTree;
import aQute.lib.io.IO;

public class ProjectFacadeGenerateTest {
	@InjectTemporaryDirectory
	File tmp;

	@Test
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public void testFacadeGenerator() throws Exception {
		try (Workspace ws = getWorkspace("resources/ws-stalecheck")) {
			getRepo(ws);
			Project project = ws.getProject("p2");
			project.setProperty("-generate",
				"bnd.bnd;output=src-gen/;generate='facadegen -o src-gen/ -e biz.aQute.bnd.project.TestBase -p biz.aQute.bnd.project Function:java.util.function.Function:java.util.function.Supplier'");

			File outputdir = project.getFile("src-gen");

			project.getGenerate()
				.generate(true);
			project.check();
			assertThat(IO.getFile(outputdir, "biz/aQute/bnd/project/Function.java")).isFile();
		}
	}

	private Workspace getWorkspace(File file) throws Exception {
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}

	private Workspace getWorkspace(String dir) throws Exception {
		return getWorkspace(new File(dir));
	}

	private void getRepo(Workspace ws) throws IOException, Exception {
		System.out.println("current working dir " + IO.work);
		FileTree tree = new FileTree();
		List<File> files = tree.getFiles(IO.getFile("generated/"), "*.jar");
		File jar = IO.getFile("jar/")
			.getCanonicalFile();
		File javagen = IO.getFile("../biz.aQute.bnd.javagen/generated/")
			.getCanonicalFile();

		files.addAll(tree.getFiles(jar, "*.jar"));
		files.addAll(tree.getFiles(javagen, "*.jar"));
		System.out.println("tmp repo " + files);
		FileSetRepository repo = new FileSetRepository("test", files);
		ws.addBasicPlugin(repo);
	}

}
