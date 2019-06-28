package aQute.bnd.repository.fileset;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import aQute.lib.io.IO;

public class FileSetRepositoryTest {

	@Test
	public void includesMavenArtifacts() throws Exception {
		List<File> files = Arrays.asList(IO.getFile("testresources/demo.jar"), IO.getFile("testresources/release.jar"),
			IO.getFile("testresources/nanohttpd-2.2.0.jar"), IO.getFile("testresources/jsr250-api-1.0.jar"),
			IO.getFile("testresources/javafx-base-13-ea+8-linux.jar"));

		assertThat(files).hasSizeGreaterThan(0);

		FileSetRepository repository = new FileSetRepository("test", files);

		assertThat(repository.list(null)).contains("org.nanohttpd:nanohttpd", "javafx.base")
			.doesNotContain("javax.annotation:jsr250-api");

		assertThat(repository.refresh()).isTrue();

		assertThat(repository.list(null)).contains("org.nanohttpd:nanohttpd", "javafx.base")
			.doesNotContain("javax.annotation:jsr250-api");

	}

}
