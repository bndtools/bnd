package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.MenuContainer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.service.reporter.Reporter;

@SuppressWarnings("resource")

public class PluginTest {
	private Processor main;

	@BeforeEach
	protected void setUp() {
		main = new Processor();
	}

	@Test
	public void testPluginInheritance() throws IOException {
		Processor top = new Processor();
		Processor middle = new Processor(top);
		Processor bottom = new Processor(middle);

		top.setProperty("-plugin.top", TPlugin.class.getName() + ";l=top");
		middle.setProperty("-plugin.middle", TPlugin.class.getName() + ";l=middle");

		assertThat(top.getPlugins(TPlugin.class)).hasSize(1);
		assertThat(middle.getPlugins(TPlugin.class)).hasSize(2)
			.containsAll(top.getPlugins(TPlugin.class));
		assertThat(bottom.getPlugins(TPlugin.class)).hasSize(2)
			.containsExactlyElementsOf(middle.getPlugins(TPlugin.class));
		assertThat(bottom.getPlugin(TPlugin.class)).isSameAs(middle.getPlugin(TPlugin.class))
			.isNotSameAs(top.getPlugin(TPlugin.class));
		assertThat(middle.getPlugin(TPlugin.class).properties).containsEntry("l", "middle");
		assertThat(top.getPlugin(TPlugin.class).properties).containsEntry("l", "top");
		assertThat(middle.getPlugin(TPlugin.class).reporter).isSameAs(middle);
		assertThat(top.getPlugin(TPlugin.class).reporter).isSameAs(top);
		assertTrue(top.check());
		assertTrue(middle.check());
		assertTrue(bottom.check());
	}

	@Test
	public void testMissingPluginNotUsed() throws Exception {
		Builder p = new Builder();
		p.setProperty("-plugin", "missing;command:=\"-abc,-def\"");
		/* List<?> plugins = */p.getPlugins(Object.class);
		assertTrue(p.check());

		p.setProperty("-abc", "whatever");
		p.setProperty("-resourceonly", "true");
		p.setProperty("Include-Resource", "jar/osgi.jar");
		p.build();
		assertEquals(1, p.getErrors()
			.size());
		assertTrue(p.getErrors()
			.get(0)
			.contains("Missing plugin"));
	}

	public static class TPlugin implements Plugin {
		Map<String, String>	properties;
		Reporter			reporter;

		@Override
		public void setProperties(Map<String, String> map) {
			properties = map;
		}

		@Override
		public void setReporter(Reporter processor) {
			reporter = processor;
		}
	}

	@Test
	public void testPlugin() {
		main.setProperty(Constants.PLUGIN, "test.PluginTest.TPlugin;a=1;b=2");

		for (TPlugin plugin : main.getPlugins(TPlugin.class)) {
			assertEquals(test.PluginTest.TPlugin.class, plugin.getClass());
			assertEquals("1", plugin.properties.get("a"));
			assertEquals("2", plugin.properties.get("b"));
			assertSame(main, plugin.reporter);
		}
	}

	@Test
	public void testLoadPluginFailsWithMissingPath() throws Exception {
		Builder p = new Builder();
		p.setProperty(Constants.PLUGIN, "thinlet.Thinlet");
		p.setProperty("-fixupmessages", "^Exception: ");

		p.getPlugins(Object.class);
		assertTrue(p.check("plugin thinlet\\.Thinlet"));
	}

	@Test
	public void testLoadPluginWithPath() {
		Builder p = new Builder();
		p.setProperty(Constants.PLUGIN, "thinlet.Thinlet;path:=jar/thinlet.jar");

		List<MenuContainer> plugins = p.getPlugins(MenuContainer.class);
		assertEquals(0, p.getErrors()
			.size());
		assertEquals(1, plugins.size());
		assertEquals("thinlet.Thinlet", plugins.get(0)
			.getClass()
			.getName());
	}

	@Test
	public void testLoadPluginWithGlobalPluginPath() {
		Builder p = new Builder();
		p.setProperty(Constants.PLUGIN, "thinlet.Thinlet");
		p.setProperty(Constants.PLUGINPATH, "jar/thinlet.jar");

		List<MenuContainer> plugins = p.getPlugins(MenuContainer.class);
		assertEquals(0, p.getErrors()
			.size());
		assertEquals(1, plugins.size());
		assertEquals("thinlet.Thinlet", plugins.get(0)
			.getClass()
			.getName());
	}

	@Test
	public void testLoadPluginWithGlobalPluginPathURL(@InjectTemporaryDirectory
	File tmp) throws Exception {
		try (Builder p = new Builder()) {
			p.setProperty(Constants.PLUGIN, "thinlet.Thinlet");
			p.setProperty(Constants.PLUGINPATH,
				tmp + "/tmpfile;url=file:jar/thinlet.jar;sha1=af7ec3a35b1825e678bfa80edeffe65836d55b17");

			List<MenuContainer> plugins = p.getPlugins(MenuContainer.class);
			assertEquals(0, p.getErrors()
				.size());
			assertEquals(1, plugins.size());
			assertEquals("thinlet.Thinlet", plugins.get(0)
				.getClass()
				.getName());
		}
	}
}
