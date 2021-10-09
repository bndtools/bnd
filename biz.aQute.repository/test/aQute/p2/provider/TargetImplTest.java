package aQute.p2.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.p2.api.Artifact;
import aQute.p2.packed.Unpack200;

public class TargetImplTest {

	@Test
	public void jmdns() throws Exception {
		TargetImpl impl = new TargetImpl(new Unpack200(), new HttpClient(),
			TargetImplTest.class.getResource("jmdns.target")
				.toURI(),
			Processor.getPromiseFactory());

		List<Artifact> artifacts = impl.getAllArtifacts();
		artifacts.stream()
			.map(a -> a.id + ":" + a.classifier + ":" + a.version)
			.forEach(System.out::println);

		assertEquals(1, artifacts.size());

		Artifact a = artifacts.get(0);
		assertEquals("javax.jmdns", a.id);
		assertEquals("3.5.4", a.version.toString());
	}
}
