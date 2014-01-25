package test;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.namespace.*;
import org.osgi.resource.*;

import aQute.bnd.build.*;
import aQute.bnd.build.model.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.properties.*;

public class BndEditModelTest extends TestCase {
	static CapReqBuilder	cp	= new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);

	public static void testVariableInRunRequirements() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		File f = new File("testresources/ws/p7/reuse.bndrun");
		model.setBndResource(f);
		model.setBndResourceName("reuse.bndrun");
		model.loadFrom(f);

		// VERIFY
		List<Requirement> r = model.getRunRequiresProcessed();
		assertEquals(4, r.size());
		assertEquals("(osgi.identity=variable)", r.get(0).toString());
		assertEquals("(osgi.identity=variable2)", r.get(1).toString());
		assertEquals("(osgi.identity=b)", r.get(2).toString());
		assertEquals("(osgi.identity=c)", r.get(3).toString());

		r = model.getRunRequires();
		assertEquals(3, r.size());
		assertEquals("${var}", r.get(0).toString());
		assertEquals("(osgi.identity=b)", r.get(1).toString());
		assertEquals("(osgi.identity=c)", r.get(2).toString());

		// Test Set with variables
		List<Requirement> rr = new LinkedList<Requirement>();
		CapReqBuilder cp = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
		rr.add(cp.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=b)").buildSyntheticRequirement());
		rr.add(new RequirementVariable("${var}"));
		model.setRunRequires(rr);

		// VERIFY
		r = model.getRunRequiresProcessed();
		assertEquals(3, r.size());
		assertEquals("(osgi.identity=b)", r.get(0).toString());
		assertEquals("(osgi.identity=variable)", r.get(1).toString());
		assertEquals("(osgi.identity=variable2)", r.get(2).toString());

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=variable)"), r.get(1));
		assertEquals(getReq("(osgi.identity=variable2)"), r.get(2));

		r = model.getRunRequires();
		assertEquals(2, r.size());
		assertEquals("(osgi.identity=b)", r.get(0).toString());
		assertEquals("${var}", r.get(1).toString());

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));

		// Test SET
		rr = new LinkedList<Requirement>();
		rr.add(getReq("(osgi.identity=b)"));
		rr.add(getReq("(osgi.identity=c)"));
		model.setRunRequires(rr);

		// VERIFY
		r = model.getRunRequires();
		assertEquals(2, r.size());
		assertEquals("(osgi.identity=b)", r.get(0).toString());
		assertEquals("(osgi.identity=c)", r.get(1).toString());

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));

		r = model.getRunRequiresProcessed();
		assertEquals(2, r.size());
		assertEquals("(osgi.identity=b)", r.get(0).toString());
		assertEquals("(osgi.identity=c)", r.get(1).toString());

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));

		// TEST Saving changes and those changes persist...
		Document d = new Document("");
		model.saveChangesTo(d);

		r = model.getRunRequires();
		assertEquals(2, r.size());
		assertEquals("(osgi.identity=b)", r.get(0).toString());
		assertEquals("(osgi.identity=c)", r.get(1).toString());

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));

		r = model.getRunRequiresProcessed();
		assertEquals(2, r.size());
		assertEquals("(osgi.identity=b)", r.get(0).toString());
		assertEquals("(osgi.identity=c)", r.get(1).toString());

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));
	}

	private static Requirement getReq(String n) {
		return cp.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, n).buildSyntheticRequirement();
	}
}
