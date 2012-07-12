package test.baseline;

import java.io.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.bnd.differ.*;
import aQute.lib.osgi.*;
import aQute.libg.version.*;

public class BaselineTest extends TestCase {

	public void testBaslineJar() throws Exception {
		Workspace ws = new Workspace(new File("test/ws"));
		Project top = ws.getProject("p3");
		
		Builder builder = top.getBuilder(null).getSubBuilder();
		builder.set("-baseline", "org.apache.felix.configadmin;version=1.2.0");
		
		Jar older = builder.getBaselineJar();
		assertNotNull(older);

		Jar newer = builder.build();
		
		Baseline baseline = new Baseline(top, new DiffPluginImpl());
		baseline.baseline(newer, older, null);
		
		assertTrue(baseline.getSuggestedVersion().compareTo(Version.parseVersion("1.1.0")) == 0);
	}

	public void testBaslineRepoJar() throws Exception {
		Workspace ws = new Workspace(new File("test/ws"));
		Project top = ws.getProject("p3");
		
		Builder builder = top.getBuilder(null).getSubBuilder();
		builder.set("-baseline-repo", "Release");
		builder.set("Bundle-SymbolicName","org.apache.felix.configadmin;singleton:=true");

		Jar jar = builder.getBaselineJar();
		assertNotNull(jar);

	}
}
