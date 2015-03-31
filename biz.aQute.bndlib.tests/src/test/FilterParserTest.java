package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.osgi.resource.FilterParser.Expression;

public class FilterParserTest extends TestCase {
	FilterParser fp = new FilterParser();

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
