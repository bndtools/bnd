package test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.resource.Capability;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.version.Version;
import aQute.lib.filter.Filter;
import aQute.lib.strings.Strings;
import junit.framework.TestCase;

public class ParseHeaderTest extends TestCase {

	public void testTyped() {
		Parameters p = new Parameters("a;a:Long=1;b:Double=3.2;c:String=abc;d:Version=1"
			+ ";e:List<Long>='1,2,3';f:List<Double>='1.0,1.1,1.2';g:List<String>='abc,def,ghi';h:List<Version>='1.0.1,1.0.2'");

		String s = p.toString();
		System.out.println(s);
		assertEquals("a;a:Long=1;b:Double=\"3.2\";c=abc;d:Version=1;"
			+ "e:List<Long>=\"1,2,3\";f:List<Double>=\"1.0,1.1,1.2\";g:List<String>=\"abc,def,ghi\";h:List<Version>=\"1.0.1,1.0.2\"",
			s);

		Attrs attrs = p.get("a");
		assertNotNull(attrs);

		assertEquals(1L, attrs.getTyped("a"));
		assertEquals(3.2d, attrs.getTyped("b"));
		assertEquals("abc", attrs.getTyped("c"));
		assertEquals(new Version("1"), attrs.getTyped("d"));
		assertEquals(Arrays.asList(1L, 2L, 3L), attrs.getTyped("e"));
		assertEquals(Arrays.asList(1.0D, 1.1D, 1.2D), attrs.getTyped("f"));
		assertEquals(Arrays.asList("abc", "def", "ghi"), attrs.getTyped("g"));
		assertEquals(Arrays.asList(new Version("1.0.1"), new Version("1.0.2")), attrs.getTyped("h"));
	}

	public void testTypedSpaces() {
		Parameters p = new Parameters("a;a : Long=1;b:Double  =3.2;c:  String=abc;d : Version =1"
			+ ";e  :List < Long >='1,2,3';f: List<Double> ='1.0,1.1,1.2';g:List <String> ='abc,def,ghi';h  :  List  <  Version >  ='1.0.1,1.0.2'");

		String s = p.toString();
		System.out.println(s);
		assertEquals("a;a:Long=1;b:Double=\"3.2\";c=abc;d:Version=1;"
			+ "e:List<Long>=\"1,2,3\";f:List<Double>=\"1.0,1.1,1.2\";g:List<String>=\"abc,def,ghi\";h:List<Version>=\"1.0.1,1.0.2\"",
			s);

		Attrs attrs = p.get("a");
		assertNotNull(attrs);

		assertEquals(1L, attrs.getTyped("a"));
		assertEquals(3.2d, attrs.getTyped("b"));
		assertEquals("abc", attrs.getTyped("c"));
		assertEquals(new Version("1"), attrs.getTyped("d"));
		assertEquals(Arrays.asList(1L, 2L, 3L), attrs.getTyped("e"));
		assertEquals(Arrays.asList(1.0D, 1.1D, 1.2D), attrs.getTyped("f"));
		assertEquals(Arrays.asList("abc", "def", "ghi"), attrs.getTyped("g"));
		assertEquals(Arrays.asList(new Version("1.0.1"), new Version("1.0.2")), attrs.getTyped("h"));
	}

	public void testMergeWithOverrideFalse() {
		Parameters a = new Parameters("a;a=value_a;av:Version=\"1.0.0\"");
		Parameters b = new Parameters("b;b=metal;bv:Version=\"1.0.0\"");

		try {
			a.mergeWith(b, false);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	public void testEscaping() {

		{
			// Spaces at end of quoted string
			Parameters pp = new Parameters("a;string.list3:List<String>=\" aString , bString , cString \"");
			assertEquals("a;string.list3:List<String>=\" aString , bString , cString \"", pp.toString());
		}
		{
			// It should be string.list2:List="a\"quote,a\,comma, aSpace
			// ,\"start,\,start,end\",end\," (not handling escape of comma)
			Parameters pp = new Parameters(
				"a;b:List=\"a\\\"quote,a\\\\backslash,a\\,comma, aSpace ,\\\"start,\\,start\\,end\"");
			assertEquals("a;b:List<String>=\"a\\\"quote,a\\\\backslash,a\\,comma, aSpace ,\\\"start,\\,start\\,end\"",
				pp.toString());
		}

		{
			Parameters pp = new Parameters("a;a:List<String>='abc'");
			assertEquals("a;a:List<String>=abc", pp.toString());
		}

	}

	public void testPropertiesSimple() {
		Map<String, String> p = OSGiHeader.parseProperties("a=1, b=\"3   3\", c=c");
		assertEquals("c", p.get("c"));
		assertEquals("1", p.get("a"));
		assertEquals("3   3", p.get("b"));
	}

	/**
	 * #385 If you set a single Runtime Property in a bndrun with no value, it
	 * will be ignored. For example: osgi.console If you add an additional
	 * Runtime Property, then the first property with no value will be set
	 * properly. For example:: osgi.console osgi.service.http.port 8080 It also
	 * appears that order matters. The following does not work: -runproperties:
	 * osgi.service.http.port=8180,\ osgi.console= while the following does
	 * work: -runproperties: osgi.console=,\ osgi.service.http.port=8180
	 */
	public void testUnfinishedProperties() {
		Map<String, String> p = OSGiHeader.parseProperties("osgi.console");
		assertEquals("", p.get("osgi.console"));
		p = OSGiHeader.parseProperties("osgi.console=");
		assertEquals("", p.get("osgi.console"));
		p = OSGiHeader.parseProperties("osgi.console=,x=1");
		assertEquals("", p.get("osgi.console"));
		p = OSGiHeader.parseProperties("osgi.console,x=1");
		assertEquals("", p.get("osgi.console"));
		p = OSGiHeader.parseProperties("a=1,osgi.console=,x=1");
		assertEquals("", p.get("osgi.console"));
		p = OSGiHeader.parseProperties("a=1,osgi.console=");
		assertEquals("", p.get("osgi.console"));
		p = OSGiHeader.parseProperties("a=1,osgi.console");
		assertEquals("", p.get("osgi.console"));
	}

	public void testClauseName() {
		assertNames("a,b,c;", new String[] {
			"a", "b", "c"
		});
		assertNames("a,b,c", new String[] {
			"a", "b", "c"
		});
		assertNames("a;x=0,b;x=0,c;x=0", new String[] {
			"a", "b", "c"
		});
		assertNames("a;b;c;x=0", new String[] {
			"a", "b", "c"
		});
		assertNames(",", new String[] {}, null, "Empty clause, usually caused");
		assertNames("a;a,b", new String[] {
			"a", "a~", "b"
		}, null, "Duplicate name a used in header");
		assertNames("a;x=0;b", new String[] {
			"a", "b"
		}, "Header contains name field after attribute or directive", null);
		assertNames("a;x=0;x=0,b", new String[] {
			"a", "b"
		}, null, "Duplicate attribute/directive name");
		assertNames("a;;;,b", new String[] {
			"a", "b"
		});
		assertNames(",,a,,", new String[] {
			"a"
		}, null, "Empty clause, usually caused by repeating");
		assertNames(",a", new String[] {
			"a"
		}, null, "Empty clause, usually caused");
		assertNames(",a,b,c,", new String[] {
			"a", "b", "c"
		}, null, "Empty clause, usually caused");
		assertNames("a,b,c,", new String[] {
			"a", "b", "c"
		}, null, "Empty clause, usually caused");
		assertNames("a,b,,c", new String[] {
			"a", "b", "c"
		}, null, "Empty clause, usually caused");
	}

	static void assertNames(String header, String[] keys) {
		assertNames(header, keys, null, null);
	}

	static void assertNames(String header, String[] keys, String expectedError, String expectedWarning) {
		Processor p = new Processor();
		p.setPedantic(true);
		Parameters map = Processor.parseHeader(header, p);
		for (String key : keys)
			assertTrue(map.containsKey(key));

		assertEquals(keys.length, map.size());
		if (expectedError != null) {
			System.err.println(p.getErrors());
			assertTrue(p.getErrors()
				.size() > 0);
			assertTrue(p.getErrors()
				.get(0)
				.contains(expectedError));
		} else
			assertEquals(0, p.getErrors()
				.size());
		if (expectedWarning != null) {
			System.err.println(p.getWarnings());
			assertTrue(p.getWarnings()
				.size() > 0);
			String w = p.getWarnings()
				.get(0);
			assertTrue(w.contains(expectedWarning));
		} else
			assertEquals(0, p.getWarnings()
				.size());
	}

	public void testSimple() {
		String s = "a;a=a1;b=a2;c=a3, b;a=b1;b=b2;c=b3, c;d;e;a=x1";
		Parameters map = Processor.parseHeader(s, null);
		assertEquals(5, map.size());

		Map<String, String> a = map.get("a");
		assertEquals("a1", a.get("a"));
		assertEquals("a2", a.get("b"));
		assertEquals("a3", a.get("c"));

		Map<String, String> d = map.get("d");
		assertEquals("x1", d.get("a"));

		Map<String, String> e = map.get("e");
		assertEquals(e, d);

		System.err.println(map);
	}

	public void testParseMultiValueAttribute() {
		String s = "capability;foo:List<String>=\"MacOSX,Mac OS X\";version:List<Version>=\"1.0, 2.0, 2.1\"";
		Parameters map = Processor.parseHeader(s, null);

		Attrs attrs = map.get("capability");

		assertEquals(Type.STRINGS, attrs.getType("foo"));
		List<String> foo = attrs.getTyped(Attrs.LIST_STRING, "foo");
		assertEquals(2, foo.size());
		assertEquals("MacOSX", foo.get(0));
		assertEquals("Mac OS X", foo.get(1));

		assertEquals(Type.VERSIONS, attrs.getType("version"));
		List<Version> version = attrs.getTyped(Attrs.LIST_VERSION, "version");
		assertEquals(3, version.size());
		assertEquals(new Version(1), version.get(0));
		assertEquals(new Version(2), version.get(1));
		assertEquals(new Version(2, 1), version.get(2));
	}

	public void testParametersCollector() throws Exception {
		Stream<String> pkgs = Stream.of("com.foo;com.fuu;fizz=bazz;dir:=dar", "com.bar;a=b,org.foo;provide:=true",
			"org.fuu", "io.hiho;viking:Version=2");
		Parameters p = pkgs.collect(Parameters.toParameters());
		Attrs a;
		a = p.get("com.foo");
		assertNotNull(a);
		assertEquals("bazz", a.get("fizz"));
		assertEquals("dar", a.get("dir:"));
		a = p.get("com.fuu");
		assertNotNull(a);
		assertEquals("bazz", a.get("fizz"));
		assertEquals("dar", a.get("dir:"));
		a = p.get("com.bar");
		assertNotNull(a);
		assertEquals("b", a.get("a"));
		assertNull(a.get("provide:"));
		a = p.get("org.foo");
		assertNotNull(a);
		assertNull(a.get("a"));
		assertEquals("true", a.get("provide:"));
		a = p.get("org.fuu");
		assertNotNull(a);
		assertTrue(a.isEmpty());
		a = p.get("io.hiho");
		assertNotNull(a);
		assertEquals(Attrs.Type.VERSION, a.getType("viking"));
		assertEquals(Version.parseVersion("2.0.0"), Version.parseVersion(a.get("viking")));
	}

	public void testParametersKeyList() throws Exception {
		String s = "--add-opens, mod1, --add-opens, mod2";
		Parameters map = Processor.parseHeader(s, null);
		Collection<String> keyList = map.keyList();
		assertEquals("--add-opens mod1 --add-opens mod2", Strings.join(" ", keyList));
	}

	@SuppressWarnings({
		"null", "unchecked"
	})
	public void testParseListAttributesAndMatchWithFilter() throws Exception {
		List<String> urls = null;
		Filter f = new Filter("(url=http://three)");
		Parameters p = new Parameters("foo;url:List<String>='http://one,http://two,http://three'");
		for (Map.Entry<String, Attrs> entry : p.entrySet()) {
			CapReqBuilder req = new CapReqBuilder(entry.getKey(), entry.getValue());
			Capability c = req.buildSyntheticCapability();
			if (f.matchMap(c.getAttributes())) {
				urls = (List<String>) c.getAttributes()
					.get("url");
			}
		}
		assertNotNull(urls);
		assertEquals(3, urls.size());
	}

	@SuppressWarnings({
		"null", "unchecked"
	})
	public void testParseListAttributesAndMatchWithFilter_b() throws Exception {
		List<String> urls = null;
		Filter f = new Filter("(url=http://three)");
		Parameters p = new Parameters("foo;url:List<String>='http://one,http://two,http://three'");
		for (Map.Entry<String, Attrs> entry : p.entrySet()) {
			CapReqBuilder req = new CapReqBuilder(entry.getKey());
			req.addAttributesOrDirectives(entry.getValue());
			Capability c = req.buildSyntheticCapability();
			if (f.matchMap(c.getAttributes())) {
				urls = (List<String>) c.getAttributes()
					.get("url");
			}
		}
		assertNotNull(urls);
		assertEquals(3, urls.size());
	}
}
