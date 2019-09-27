package test;

import java.io.IOException;

import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.FilterParser.WithRangeExpression;
import junit.framework.TestCase;

public class FilterParserTest extends TestCase {
	FilterParser fp = new FilterParser();

	public void testSpaces() throws Exception {
		FilterParser p = new FilterParser();
		p.parse("(| (a=b) (b=c) )");
		p.parse("(| (&(osgi.ee=JavaSE)(version=1.6)) (&(osgi.ee=JavaSE/compact1)(version=1.8)) )");
		p.parse("(  &  (   org.osgi.framework.windowing.system   =   xyz    )     )");
		p.parse("( | ( org.osgi.framework.windowing.system=xyz))");
	}

	public void testNestedAnd() throws IOException {
		aQute.bnd.osgi.resource.FilterParser.Expression exp = fp
			.parse("(&(osgi.wiring.package=osgi.enroute.webserver)(&(version>=1.0.0)(!(version>=2.0.0))))");
		System.out.println(exp);
	}

	public void testSimple() throws IOException {
		aQute.bnd.osgi.resource.FilterParser.Expression exp = fp
			.parse("(&(osgi.wiring.package=package)(|(|(c=4)))(!(version>=2.0.0))(!(a=3))(version>=1.0.0))");
		System.out.println(exp);
	}

	public void testReduce() throws IOException {
		Expression exp = fp.parse("(&(osgi.wiring.package=package)(!(version>=2.0.0))(version>=1.0.0))");
		System.out.println(exp);
	}

	public void testVoidRange() throws IOException {
		Expression exp = fp.parse("(&(osgi.wiring.package=package)(version>=0.0.0))");
		System.out.println(exp);
	}

	public void testPackageRange() throws Exception {
		Expression expression = fp.parse("(&(osgi.wiring.package=A)(version>=1.0.0)(!(version>=2.0.0)))");
		assertTrue(expression instanceof WithRangeExpression);
		assertEquals("[1.0.0,2.0.0)", ((WithRangeExpression) expression).getRangeExpression()
			.getRangeString());
	}

	public void testBundleRange() throws Exception {
		Expression expression = fp.parse("(&(osgi.wiring.bundle=B)(bundle-version>=1.0.0)(!(bundle-version>=2.0.0)))");
		assertTrue(expression instanceof WithRangeExpression);
		assertEquals("[1.0.0,2.0.0)", ((WithRangeExpression) expression).getRangeExpression()
			.getRangeString());
	}

	public void testIdentity() throws IOException {
		Expression exp = fp.parse("(&(osgi.identity=identity)(version>=0.0.0))");
		System.out.println(exp);
		exp = fp.parse("(&(osgi.identity=identity)(!(version>=2.0.0))(version>=1.0.0))");
		System.out.println(exp);
	}

	/**
	 * Since the filters are cached we need to get similar filters to check if
	 * this works.
	 *
	 * @throws IOException
	 */
	public void testCache() throws IOException {
		Expression exp = fp.parse("(&(osgi.wiring.package=a)(version>=1)(!(version>=2.0.0)))");
		Expression exp2 = fp.parse("(&(osgi.wiring.package=b)(version>=1.1)(!(version>=2.0.0)))");
		assertNotNull(exp2);
		assertNotNull(exp);
	}
}
