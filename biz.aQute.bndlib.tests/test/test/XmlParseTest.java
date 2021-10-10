package test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class XmlParseTest {
	@Test
	public void testSimple() throws Exception {
		Builder b = new Builder();
		b.setProperty("-plugin", "aQute.lib.spring.SpringXMLType");
		b.setProperty("-resourceonly", "true");
		b.setProperty("-includeresource", "OSGI-INF/blueprint/x.xml=testresources/blueprint.xml");
		Jar jar = b.build();
		assertTrue(b.check());
		assertTrue(b.getImports()
			.containsFQN("a.b"));
		assertTrue(b.getImports()
			.containsFQN("d.e"));

		b.close();
	}
}
