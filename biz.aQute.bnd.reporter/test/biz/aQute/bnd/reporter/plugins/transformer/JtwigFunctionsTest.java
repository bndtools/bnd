package biz.aQute.bnd.reporter.plugins.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jtwig.functions.FunctionRequest;
import org.jtwig.render.RenderRequest;

import junit.framework.TestCase;

public class JtwigFunctionsTest extends TestCase {

	class FRMock extends FunctionRequest {

		private String				name;
		private boolean				defaultSection;
		private Map<String, Object>	map;

		public FRMock(String name, boolean defaultSection, Map<String, Object> map) {
			super(new RenderRequest(null, null), null, "", null);
			this.name = name;
			this.defaultSection = defaultSection;
			this.map = map;
		}

		@Override
		public Object get(int index) {

			switch (index) {
				case 0 :
					return name;
				case 1 :
					return defaultSection;
				case 2 :
					return map;
				default :
					break;
			}
			return index;
		}

	}

	public void testNoParamDefaltTrue() throws Exception {

		Object actual = JTwigFunctions.newfunction_showSection()
			.execute(new FRMock("", true, new HashMap<String, Object>()));

		assertEquals(true, actual);
	}

	public void testNoParamDefaltFalse() throws Exception {

		Object actual = JTwigFunctions.newfunction_showSection()
			.execute(new FRMock("", false, new HashMap<String, Object>()));

		assertEquals(false, actual);
	}

	public void testNullMap() throws Exception {

		Object actual = JTwigFunctions.newfunction_showSection()
			.execute(new FRMock("", true, null));

		assertEquals(true, actual);
	}

	public void test1() throws Exception {

		Map<String, Object> map = new TreeMap<String, Object>();
		map.put(JTwigFunctions.BND_REPORTER_SHOW_PREFIX + "1.1.*", "true");
		map.put(JTwigFunctions.BND_REPORTER_SHOW_PREFIX + "1.1.1", "false");

		assertEquals(false, JTwigFunctions.newfunction_showSection()
			.execute(new FRMock("1.1.1", false, map)));
		assertEquals(true, JTwigFunctions.newfunction_showSection()
			.execute(new FRMock("1.1.2", false, map)));
	}

	public void test2() throws Exception {

		Map<String, Object> map = new TreeMap<String, Object>();
		map.put(JTwigFunctions.BND_REPORTER_SHOW_PREFIX + "1.*", "false");

		assertEquals(false, JTwigFunctions.newfunction_showSection()
			.execute(new FRMock("1.1.1", true, map)));
	}

}
