package aQute.bnd.repository.maven.provider;
import java.net.URI;

import org.junit.Test;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.maven.provider.Crawler;
import aQute.lib.strings.Strings;

public class CrawlerTest {
	@Test
	public void testTravers() throws Exception {
		HttpClient client = new HttpClient();
		PromiseFactory promiseFactory = Processor.getPromiseFactory();
		Crawler c = new Crawler(client, promiseFactory);
		System.out
			.println(Strings.join("\n", c.getURIs(new URI("https://bndtools.jfrog.io/bndtools/libs-release-local/"),
				Crawler.predicate("/5.0.0/", null))));
	}

}
