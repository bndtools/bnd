package aQute.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;
import aQute.p2.api.Artifact;
import aQute.p2.packed.Unpack200;

public class P2ImplTest {
	@InjectTemporaryDirectory
	File tmp;

	@Disabled("The referenced p2 site, http://www.jmdns.org/update-site/3.5.4/, does not exist")
	@Test
	public void test() throws URISyntaxException, Exception {
		P2Impl p2 = new P2Impl(new Unpack200(), new HttpClient(), new URI("http://www.jmdns.org/update-site/3.5.4/"),
			Processor.getPromiseFactory());

		p2.getArtifacts()
			.forEach(System.out::println);
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	public void testOpaqueWindowsFileUriRepository() throws Exception {
		File source = IO.getFile("testdata/p2/macbadge");
		assertThat(source).isDirectory();

		File repo = IO.getFile(tmp, "repository");
		IO.copy(source, repo);

		String windowsStylePath = repo.getAbsolutePath()
			.replace('\\', '/');
		URI opaqueFileUri = new URI("file:" + windowsStylePath);
		assertThat(opaqueFileUri.getPath()).isNull();

		try (HttpClient client = new HttpClient()) {
			client.setCache(IO.getFile(tmp, "cache"));
			P2Impl p2 = new P2Impl(new Unpack200(), client, opaqueFileUri, Processor.getPromiseFactory());
			List<Artifact> artifacts = p2.getArtifacts();
			assertThat(artifacts).isNotEmpty();
			assertThat(artifacts).anyMatch(a -> "name.njbartlett.eclipse.macbadge".equals(a.id));
		}
	}
}
