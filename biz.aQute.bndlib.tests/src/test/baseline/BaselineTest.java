package test.baseline;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.osgi.*;

// TODO needs work because it does not test anything
public class BaselineTest extends TestCase {

	public static void testBaslineJar() throws Exception {
		Workspace ws = new Workspace(new File("test/ws"));
		Project top = ws.getProject("p3");
		
		Builder builder = top.getBuilder(null).getSubBuilder();
		builder.set("-baseline", "org.apache.felix.configadmin;version=1.2.0");
		
	}
}
