package aQute.bnd.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

public class ContainerTest {
	@Test
	public void testExplodedBundle(@TempDir
	Path tempDir) throws Exception {
		try (Jar jar = new Jar("testme"); Analyzer analyzer = new Analyzer(jar)) {
			jar.setManifest(analyzer.calcManifest());
			jar.writeFolder(tempDir.toFile());
		}
		Container container = new Container(tempDir.toFile(), null);
		assertThat(container.getManifest()).isNotNull();
	}
}
