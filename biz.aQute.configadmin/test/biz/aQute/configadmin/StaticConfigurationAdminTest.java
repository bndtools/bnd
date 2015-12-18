package biz.aQute.configadmin;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.osgi.service.cm.Configuration;

import junit.framework.TestCase;

public class StaticConfigurationAdminTest extends TestCase {

	public void testCreateAndList() throws Exception {
		List<StaticConfiguration> configs = new LinkedList<StaticConfiguration>();

		Dictionary<String,Object> dict = new Hashtable<String,Object>();
		dict.put("foo", "bar");
		configs.add(StaticConfiguration.createSingletonConfiguration("org.example.foo", dict));
		configs.add(StaticConfiguration.createSingletonConfiguration("org.example.bar", dict));

		StaticConfigurationAdmin cm = new StaticConfigurationAdmin(null, configs);
		Configuration[] result = cm.listConfigurations(null);

		assertEquals(2, result.length);
		assertEquals(configs.get(0), result[0]);
		assertEquals(configs.get(1), result[1]);
	}

	public void testFilter() throws Exception {
		List<StaticConfiguration> configs = new LinkedList<StaticConfiguration>();

		Dictionary<String,Object> dict;

		dict = new Hashtable<String,Object>();
		dict.put("foo", "bar");
		configs.add(StaticConfiguration.createSingletonConfiguration("org.example.foo", dict));

		dict = new Hashtable<String,Object>();
		dict.put("foo", "baz");
		configs.add(StaticConfiguration.createSingletonConfiguration("org.example.bar", dict));

		StaticConfigurationAdmin cm = new StaticConfigurationAdmin(null, configs);
		Configuration[] result = cm.listConfigurations("(foo=baz)");

		assertEquals(1, result.length);
		assertEquals(configs.get(1), result[0]);
	}

	public void testGet() throws Exception {
		List<StaticConfiguration> configs = new LinkedList<StaticConfiguration>();

		Dictionary<String,Object> dict;

		dict = new Hashtable<String,Object>();
		dict.put("foo", "bar");
		configs.add(StaticConfiguration.createSingletonConfiguration("org.example.foo", dict));

		dict = new Hashtable<String,Object>();
		dict.put("foo", "baz");
		configs.add(StaticConfiguration.createSingletonConfiguration("org.example.bar", dict));

		StaticConfigurationAdmin cm = new StaticConfigurationAdmin(null, configs);

		assertEquals(configs.get(0), cm.getConfiguration("org.example.foo"));
		assertEquals(configs.get(0), cm.getConfiguration("org.example.foo", null));
		assertEquals(configs.get(0), cm.getConfiguration("org.example.foo", "arbitrary_location"));

		try {
			cm.getConfiguration("org.example.baz");
			fail("Should throw SecurityException");
		} catch (SecurityException e) {
			// expected
		}
	}
}
