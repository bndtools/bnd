package aQute.lib.config.proxy;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ConfigurationProxyTest {

	interface T1_Bare {
		String foo();
	}

	@interface T1_AnnnotationWithDefault {
		String foo() default "foo";
	}

	@interface T1_AnnnotationWithoutDefault {
		String foo();
	}

	@Test
	public void testConfigurationProxyBare() {
		Map<String, Object> map = new HashMap<>();
		T1_Bare handle = ConfigurationProxy.create(T1_Bare.class, map);
		assertEquals(handle.foo(), null);
		map.put("?.foo", "FOO");
		assertEquals(handle.foo(), "FOO");
		map.put("foo", "BAR");
		assertEquals(handle.foo(), "BAR");
	}

	@Test
	public void testConfigurationProxyAnnotationWithDefault() {
		Map<String, Object> map = new HashMap<>();
		T1_AnnnotationWithDefault handle = ConfigurationProxy.create(T1_AnnnotationWithDefault.class, map);
		assertEquals(handle.foo(), "foo");
		map.put("?.foo", "FOO");
		assertEquals(handle.foo(), "FOO");
		map.put("foo", "BAR");
		assertEquals(handle.foo(), "BAR");
	}

	@Test
	public void testConfigurationProxyAnnotationWithoutDefault() {
		Map<String, Object> map = new HashMap<>();
		T1_AnnnotationWithoutDefault handle = ConfigurationProxy.create(T1_AnnnotationWithoutDefault.class, map);
		assertEquals(handle.foo(), null);
		map.put("?.foo", "FOO");
		assertEquals(handle.foo(), "FOO");
		map.put("foo", "BAR");
		assertEquals(handle.foo(), "BAR");
	}
}
