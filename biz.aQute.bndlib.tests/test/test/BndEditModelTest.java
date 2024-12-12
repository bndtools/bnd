package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.properties.Document;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

public class BndEditModelTest {
	static CapReqBuilder cp = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);

	@Test
	public void testVariableInRunRequirements() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		File f = new File("testresources/ws/p7/reuse.bndrun");
		model.setBndResource(f);
		model.setBndResourceName("reuse.bndrun");
		model.loadFrom(f);

		// VERIFY
		Processor processor = model.getProperties();
		String runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		String[] rrr = runrequirements.split(",");
		assertEquals(4, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable)'", rrr[0]);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable2)'", rrr[1]);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", rrr[2]);
		assertEquals("osgi.identity;filter:='(osgi.identity=c)'", rrr[3]);

		// [cs] don't know how to update this.
		List<Requirement> r = model.getRunRequires();
		assertEquals(3, r.size());
		assertEquals(new CapReqBuilder("${var}").buildSyntheticRequirement(), r.get(0));
		assertEquals(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=b)")
			.buildSyntheticRequirement(), r.get(1));
		assertEquals(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=c)")
			.buildSyntheticRequirement(), r.get(2));

		// Test Set with variables
		List<Requirement> rr = new LinkedList<>();
		rr.add(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=b)")
			.buildSyntheticRequirement());
		rr.add(new CapReqBuilder("${var}").buildSyntheticRequirement());
		model.setRunRequires(rr);

		// VERIFY
		processor = model.getProperties();
		runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		rrr = runrequirements.split(",");
		assertEquals(3, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", rrr[0]);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable)'", rrr[1]);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable2)'", rrr[2]);

		// Test SET
		rr = new LinkedList<>();
		rr.add(getReq("(osgi.identity=b)"));
		rr.add(getReq("(osgi.identity=c)"));
		model.setRunRequires(rr);

		// VERIFY
		processor = model.getProperties();
		runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		rrr = runrequirements.split(",");
		assertEquals(2, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", rrr[0]);
		assertEquals("osgi.identity;filter:='(osgi.identity=c)'", rrr[1]);

		r = model.getRunRequires();
		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));

		// TEST Saving changes and those changes persist...
		Document d = new Document("");
		model.saveChangesTo(d);

		model.loadFrom(d); // reset the properties

		runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		rrr = runrequirements.split(",");
		assertEquals(2, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", Strings.trim(rrr[0]));
		assertEquals("osgi.identity;filter:='(osgi.identity=c)'", Strings.trim(rrr[1]));

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));
	}

	private static Requirement getReq(String n) {
		return cp.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, n)
			.buildSyntheticRequirement();
	}

	@Test
	public void testVariableInSystemPackages() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		File f = new File("testresources/ws/p7/syspkg.bndrun");
		model.setBndResource(f);
		model.setBndResourceName("syspkg.bndrun");
		model.loadFrom(f);

		List<ExportedPackage> ep = model.getSystemPackages();

		assertEquals("com.sun.xml.internal.bind", model.getProperties()
			.mergeProperties(Constants.RUNSYSTEMPACKAGES));

		ExportedPackage e = new ExportedPackage("testing", null);
		ep = new LinkedList<>();
		ep.add(e);

		model.setSystemPackages(ep);

		ep = model.getSystemPackages();
		assertEquals(1, ep.size());
		assertEquals("testing", ep.get(0)
			.getName());

		e = new ExportedPackage("${var}", null);
		ep = new LinkedList<>();
		ep.add(e);

		model.setSystemPackages(ep);

		ep = model.getSystemPackages();
		assertEquals(1, ep.size());
		assertEquals("com.sun.xml.internal.bind", model.getProperties()
			.mergeProperties(Constants.RUNSYSTEMPACKAGES));
	}

	@Test
	public void testRemovePropertyFromStandalone() throws Exception {
		File runFile = IO.getFile("testresources/standalone.bndrun");
		Run run = Run.createRun(null, runFile);

		BndEditModel model = new BndEditModel();
		model.setWorkspace(run.getWorkspace());
		model.loadFrom(runFile);

		assertEquals("A", model.getProperties()
			.get("a"));
		assertEquals("B", model.getProperties()
			.get("b"));
		assertEquals("C", model.getProperties()
			.get("c"));

		String newContent = "-standalone\n" + "a: A\n" + "c: C"; // remove b
		model.loadFrom(new ByteArrayInputStream(newContent.getBytes()));

		assertEquals("A", model.getProperties()
			.get("a"));
		assertNull(model.getProperties()
			.get("b"), "removed property should be null");
		assertEquals("C", model.getProperties()
			.get("c"));
	}

	@Test
	public void testGetProperties() throws Exception {
		File runFile = IO.getFile("testresources/standalone.bndrun");
		Run run = Run.createRun(null, runFile);
		BndEditModel model = new BndEditModel(run);

		assertThat(model.getWorkspace()).isNotNull();
		assertThat(model.getOwner()).isNotNull();

		model.setGenericString("a", "AA");

		Processor properties = model.getProperties();

		assertThat(properties.getProperty("a")).isEqualTo("AA");
		assertThat(run.getProperty("a")).isEqualTo("A");

		assertThat(properties.getPropertiesFile()).isEqualTo(run.getPropertiesFile());
		assertThat(properties.getBase()).isEqualTo(run.getBase());

	}

	@Test
	public void testGetPropertiesWithFileReplace() throws Exception {
		File runFile = IO.getFile("testresources/standalone.bndrun");
		Run run = Run.createRun(null, runFile);
		BndEditModel model = new BndEditModel(run);

		model.setGenericString("here.also", "${.}");

		Processor properties = model.getProperties();

		assertThat(properties.getProperty("here")).isEqualTo(getPortablePath(runFile.getParentFile()));

		assertThat(properties.getProperty("here.also")).isEqualTo(getPortablePath(runFile.getParentFile()));
	}

	@Test
	public void testGetPropertiesWithWorkspaceMacros() throws Exception {
		try (Workspace ws = new Workspace(new File("testresources/ws"))) {
			Project project = ws.getProject("p1");
			BndEditModel model = new BndEditModel(project);
			model.setGenericString("ws", "${workspace}");
			model.setGenericString("pro", "${project}");
			Processor p = model.getProperties();

			assertThat(p.getProperty("ws")).isEqualTo(getPortablePath(ws.getBase()));

			assertThat(p.getProperty("pro")).isEqualTo(getPortablePath(project.getBase()));
		}
	}

	@Test
	public void testGetPropertiesWithoutParent() throws Exception {
		BndEditModel model = new BndEditModel();
		model.setGenericString("foo", "FOO");
		Processor p = model.getProperties();
		assertThat(p.getProperty("foo")).isEqualTo("FOO");
		assertThat(p.getPropertyKeys(false)).contains("foo");

		model.loadFrom("");
		p = model.getProperties();
		assertThat(p.getProperty("foo")).isNull();
		assertThat(p.getPropertyKeys(false)).doesNotContain("foo");

	}

	@Test
	public void testGetPropertiesWithOnlyWorkspace() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		model.setGenericString("foo", "FOO");
		Processor p = model.getProperties();

		assertThat(p.getProperty("foo")).isEqualTo("FOO");
	}

	/**
	 * Test the testresources/bndtools-resolve-reproducer project (m2). There
	 * were issues with the inheritance and inclusion of properties.
	 */

	static final File					REPRODUCER		= IO.getFile("testresources/bndtools-resolve-reproducer");
	static final File					DEBUG_BNDRUN	= IO.getFile(REPRODUCER, "debug.bndrun");
	static final Map<String, String>	PROPERTIES		= Map.of("-runbundles",
		"org.apache.felix.gogo.command;version='[1.1.2,1.1.3)',org.apache.felix.gogo.runtime;version='[1.1.6,1.1.7)',org.apache.felix.gogo.shell;version='[1.1.4,1.1.5)'",		//
		"-resolve.effective", "active",																																			//
		"-runfw", "org.eclipse.osgi;version='3.21.0'",																															//
		"-runproperties.debug", "osgi.console=,osgi.console.enable.builtin=false",																								//
		"-runrequires", "bnd.identity;id='org.example.bndtools.bndrun.reproducer'",																								//
		"-runee", "JavaSE-17",																																					//
		"-runrequires.debug",
		"osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.runtime)',osgi.identity;filter:='(osgi.identity=org.apache.felix.gogo.command)'"							//

	);

	@Test
	public void testBasicReproducer() throws Exception {
		assertThat(DEBUG_BNDRUN).isFile();
		Run run = Run.createRun(null, DEBUG_BNDRUN);
		assertThat(run.getProperties()).containsExactlyInAnyOrderEntriesOf(PROPERTIES);
		BndEditModel model = new BndEditModel(run);

		assertThat(model.getAllPropertyNames()).containsExactlyInAnyOrder("-runbundles", "-runrequires.debug",
			"-runproperties.debug", "-include");

	}

	@Test
	public void testBasicReproducerRemove() throws Exception {
		assertThat(DEBUG_BNDRUN).isFile();
		Run run = Run.createRun(null, DEBUG_BNDRUN);
		BndEditModel model = new BndEditModel(run);

		assertThat(model.getProperties()
			.getProperties()).containsKey("-runbundles");
		assertThat(model.getDocumentChanges()).isEmpty();
		assertThat(model.getDocumentProperties()
			.keySet()).containsExactlyInAnyOrder("-runbundles", "-runrequires.debug", "-runproperties.debug",
				"-include");

		model.setRunBundles(Collections.emptyList());

		assertThat(model.getRunBundles()).isEmpty();
		assertThat(model.getDocumentChanges()).containsKey("-runbundles");
		assertThat(model.getProperties()
			.getProperties()).doesNotContainKey("-runbundles");

		Document d = new Document(IO.collect(DEBUG_BNDRUN));
		assertThat(d.get()).contains("-runbundles");

		model.saveChangesTo(d);

		assertThat(model.getDocumentChanges()).isEmpty();
		assertThat(d.get()).doesNotContain("-runbundles");
		assertThat(model.getDocumentProperties()
			.keySet()).containsExactlyInAnyOrder("-runrequires.debug", "-runproperties.debug", "-include");
	}

	@Test
	public void testBasicReproducerAdd() throws Exception {
		assertThat(DEBUG_BNDRUN).isFile();
		Run run = Run.createRun(null, DEBUG_BNDRUN);
		BndEditModel model = new BndEditModel(run);

		assertThat(model.getProperties()
			.getProperties()).doesNotContainKey("-runframework");

		model.setRunFramework("none");

		assertThat(model.getDocumentProperties()).doesNotContainKey("-runframework");

		assertThat(model.getProperties()
			.getProperties()).containsKey("-runframework");

		assertThat(model.getRunFramework()).isEqualTo("none");
		Document d = new Document(IO.collect(DEBUG_BNDRUN));
		assertThat(d.get()).doesNotContain("-runframework");
		model.saveChangesTo(d);
		assertThat(model.getDocumentChanges()).isEmpty();
		assertThat(d.get()).contains("-runframework");
	}

	private String getPortablePath(File base) {
		String path = base.getAbsolutePath();
		if (File.separatorChar != '/') {
			path = path.replace(File.separatorChar, '/');
		}
		return path;
	}

}
