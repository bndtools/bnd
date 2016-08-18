package test;

import java.awt.MenuContainer;
import java.util.List;
import java.util.Map;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;
import junit.framework.TestCase;

@SuppressWarnings("resource")

public class PluginTest extends TestCase {
	private Processor main;

	@Override
	protected void setUp() {
		main = new Processor();
	}

	public void testMissingPluginNotUsed() throws Exception {
		Builder p = new Builder();
		p.setProperty("-plugin", "missing;command:=\"-abc,-def\"");
		/* List<?> plugins = */p.getPlugins(Object.class);
		assertTrue(p.check());

		p.setProperty("-abc", "whatever");
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "jar/osgi.jar");
		p.build();
		assertEquals(1, p.getErrors().size());
		assertTrue(p.getErrors().get(0).contains("Missing plugin"));
	}

	static class TPlugin implements Plugin {
		Map<String,String> properties;
		Reporter			reporter;

		@Override
		public void setProperties(Map<String,String> map) {
			properties = map;
		}

		@Override
		public void setReporter(Reporter processor) {
			reporter = processor;
		}
	}

	public void testPlugin() {
		main.setProperty(Constants.PLUGIN, "test.PluginTest.TPlugin;a=1;b=2");

		for (TPlugin plugin : main.getPlugins(TPlugin.class)) {
			assertEquals(test.PluginTest.TPlugin.class, plugin.getClass());
			assertEquals("1", plugin.properties.get("a"));
			assertEquals("2", plugin.properties.get("b"));
			assertSame(main, plugin.reporter);
		}
	}

	@SuppressWarnings("deprecation")
	public void testLoadPlugin() {
		main.setProperty(Constants.PLUGIN, "thinlet.Thinlet;path:=jar/thinlet.jar");
		for (java.applet.Applet applet : main.getPlugins(java.applet.Applet.class)) {
			assertEquals("thinlet.Thinlet", applet.getClass().getName());
		}
	}

	public void testLoadPluginFailsWithMissingPath() throws Exception {
		Builder p = new Builder();
		p.setProperty(Constants.PLUGIN, "thinlet.Thinlet");
		p.setProperty("-fixupmessages", "^Exception: ");

		p.getPlugins(Object.class);
		assertTrue(p.check("plugin thinlet\\.Thinlet"));
	}

	public void testLoadPluginWithPath() {
		Builder p = new Builder();
		p.setProperty(Constants.PLUGIN, "thinlet.Thinlet;path:=jar/thinlet.jar");

		List<MenuContainer> plugins = p.getPlugins(MenuContainer.class);
		assertEquals(0, p.getErrors().size());
		assertEquals(1, plugins.size());
		assertEquals("thinlet.Thinlet", plugins.get(0).getClass().getName());
	}

	public void testLoadPluginWithGlobalPluginPath() {
		Builder p = new Builder();
		p.setProperty(Constants.PLUGIN, "thinlet.Thinlet");
		p.setProperty(Constants.PLUGINPATH, "jar/thinlet.jar");

		List<MenuContainer> plugins = p.getPlugins(MenuContainer.class);
		assertEquals(0, p.getErrors().size());
		assertEquals(1, plugins.size());
		assertEquals("thinlet.Thinlet", plugins.get(0).getClass().getName());
	}
}
