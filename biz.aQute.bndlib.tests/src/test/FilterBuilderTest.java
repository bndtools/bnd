package test;

import aQute.bnd.osgi.resource.FilterBuilder;
import aQute.bnd.version.VersionRange;
import junit.framework.TestCase;

public class FilterBuilderTest extends TestCase {
	FilterBuilder fb = new FilterBuilder();

	public void testSimple() {
		fb.eq("a", "b");
		assertEquals("(a=b)", fb.toString());
	}

	public void testAnd() {
		fb.and();
		fb.eq("a", "b");
		assertEquals("(a=b)", fb.toString());
	}

	public void testOr() {
		fb.or();
		fb.eq("a", "b");
		assertEquals("(a=b)", fb.toString());
	}

	public void testAndMultiple() {
		fb.and();
		fb.eq("a", "b");
		fb.eq("c", "d");
		assertEquals("(&(a=b)(c=d))", fb.toString());
	}

	public void testOrMultiple() {
		fb.or();
		fb.eq("a", "b");
		fb.eq("c", "d");
		assertEquals("(|(a=b)(c=d))", fb.toString());
	}

	public void testAndOrOrSimple() {
		fb.and();
		fb.or();
		fb.eq("a", "b");
		fb.eq("c", "d");
		fb.end();
		fb.or();
		fb.eq("e", "f");
		fb.eq("g", "h");
		fb.end();
		fb.eq("i", "j");
		fb.neq("k", "l");
		assertEquals("(&(|(a=b)(c=d))(|(e=f)(g=h))(i=j)(!(k=l)))", fb.toString());
	}

	public void testGt() {
		assertEquals("(!(a<=b))", fb.gt("a", "b")
			.toString());
	}

	public void testGe() {
		assertEquals("(a>=b)", fb.ge("a", "b")
			.toString());
	}

	public void testLe() {
		assertEquals("(a<=b)", fb.le("a", "b")
			.toString());
	}

	public void testApprox() {
		assertEquals("(a~=b)", fb.approximate("a", "b")
			.toString());
	}

	public void testPresent() {
		assertEquals("(a=*)", fb.isPresent("a")
			.toString());
	}

	public void testVersionRange() {
		assertEquals("(&(version>=1.0.0)(!(version>=2.0.0)))", fb.in("version", new VersionRange("[1,2)"))
			.toString());
		fb = new FilterBuilder();
		assertEquals("(&(!(version<=1.0.0))(version<=2.0.0))", fb.in("version", new VersionRange("(1,2]"))
			.toString());
	}

	public void testVersionRangeOSGi() {
		assertEquals("(&(version>=1.0.0)(!(version>=2.0.0)))",
			fb.in("version", new org.osgi.framework.VersionRange("[1,2)"))
				.toString());
		fb = new FilterBuilder();
		assertEquals("(&(!(version<=1.0.0))(version<=2.0.0))",
			fb.in("version", new org.osgi.framework.VersionRange("(1,2]"))
				.toString());
	}

	public void testAOSS() {
		fb.and();
		fb.or();
		fb.eq("a", "b");
		fb.eq("c", "d");
		fb.end();
		fb.end();

		assertEquals("(|(a=b)(c=d))", fb.toString());
	}
}
