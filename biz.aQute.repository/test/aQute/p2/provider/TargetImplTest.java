package aQute.p2.provider;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.p2.api.Artifact;

public class TargetImplTest {

	// TODO Fails on Cloudbees
	@Test
	public void test() throws Exception {
		// TargetImpl impl = new TargetImpl(new HttpClient(),
		// TargetImplTest.class.getResource("target.platform")
		// .toURI(),
		// Processor.getPromiseFactory());
		//
		// List<Artifact> artifacts = impl.getAllArtifacts();
		// artifacts.stream()
		// .map(a -> a.id + ":" + a.classifier + ":" + a.version)
		// .forEach(System.out::println);
	}

	@Test
	public void jmdns() throws Exception {
		TargetImpl impl = new TargetImpl(new HttpClient(), TargetImplTest.class.getResource("jmdns.target")
			.toURI(), Processor.getPromiseFactory());

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
