package test;

import junit.framework.*;
import aQute.bnd.osgi.*;

public class XmlParseTest extends TestCase {
	public void testSimple() throws Exception {
		Builder b = new Builder();
		b.setProperty("-plugin", "aQute.lib.spring.SpringXMLType");
		b.setProperty("-resourceonly", "true");
		b.setProperty("-includeresource", "OSGI-INF/blueprint/x.xml=testresources/blueprint.xml");
		Jar jar = b.build();
		assertTrue(b.check());
		assertTrue( b.getImports().containsFQN("a.b"));
		assertTrue( b.getImports().containsFQN("d.e"));
		
		b.close();
	}
}
