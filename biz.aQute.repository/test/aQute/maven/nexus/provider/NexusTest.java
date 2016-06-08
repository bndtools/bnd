package aQute.maven.nexus.provider;

import java.io.File;
import java.net.URI;
import java.util.List;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import junit.framework.TestCase;

public class NexusTest extends TestCase {


	public void testTravers() throws Exception {
		HttpClient client = new HttpClient();
		client.readSettings(new Processor());
		Nexus nexus = new Nexus(new URI("https://oss.sonatype.org/service/local/repositories/orgosgi-1073"),
				client);

		List<URI> files = nexus.files();
		System.out.println(Strings.join("\n", files));
	}

	public void testSign() throws Exception {
		Signer signer = new Signer("jokulsarlon6128".toCharArray());
		File in = IO.getFile("testresources/nexus/testfile.txt");
		signer.sign(in);
	}
}
