package aQute.p2.provider;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.p2.packed.Unpack200;

public class P2ImplTest {

	@Disabled("The referenced p2 site, http://www.jmdns.org/update-site/3.5.4/, does not exist")
	@Test
	public void test() throws URISyntaxException, Exception {
		P2Impl p2 = new P2Impl(new Unpack200(), new HttpClient(), new URI("http://www.jmdns.org/update-site/3.5.4/"),
			Processor.getPromiseFactory());

		p2.getArtifacts()
			.forEach(System.out::println);
	}
}
