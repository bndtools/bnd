package test.lib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aQute.bnd.service.Registry;

public class MockRegistry implements Registry {

	private final Set<Object> plugins = new HashSet<>();

	public void addPlugin(Object plugin) {
		plugins.add(plugin);
	}

	@Override
	public <T> List<T> getPlugins(Class<T> clazz) {
		List<T> l = new ArrayList<>();
		for (Object plugin : plugins) {
			if (clazz.isInstance(plugin))
				l.add(clazz.cast(plugin));
		}
		return l;
	}

	@Override
	public <T> T getPlugin(Class<T> clazz) {
		for (Object plugin : plugins) {
			if (clazz.isInstance(plugin))
				return clazz.cast(plugin);
		}
		return null;
	}

}
