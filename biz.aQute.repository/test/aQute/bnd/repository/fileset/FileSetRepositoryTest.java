package aQute.bnd.repository.fileset;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import aQute.lib.io.IO;

public class FileSetRepositoryTest {

	@Test
	public void includesMavenArtifacts() throws Exception {
		List<File> files = Arrays.asList(
			IO.getFile("testresources/demo.jar"), IO.getFile("testresources/release.jar"),
			IO.getFile("testresources/nanohttpd-2.2.0.jar"),
			IO.getFile("testresources/jsr250-api-1.0.jar"), IO.getFile("testresources/javafx-base-13-ea+8-linux.jar"));

		assertTrue(files.size() > 0);

		FileSetRepository repository = new FileSetRepository("test", files);

		List<String> list = repository.list(null);

		assertFalse(list.contains("javax.annotation:jsr250-api"));
		assertTrue(list.contains("org.nanohttpd:nanohttpd"));
		assertTrue(list.contains("javafx.base"));
	}

}
