package aQute.bnd.build;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

import org.osgi.resource.Capability;

import aQute.bnd.service.externalplugin.ExternalPluginNamespace;
import aQute.bnd.service.result.FunctionWithException;
import aQute.bnd.service.result.Result;
import aQute.lib.exceptions.Exceptions;

class WorkspaceExternalPluginHandler implements AutoCloseable {

	final Workspace workspace;

	WorkspaceExternalPluginHandler(Workspace workspace) {
		this.workspace = workspace;
	}

	<T, R> Result<R, String> call(String pluginName, Class<T> c, FunctionWithException<T, Result<R, String>> f) {
		try {

			String filter = ExternalPluginNamespace.filter(pluginName, c);

			Optional<Capability> optCap = workspace
				.findProviders(ExternalPluginNamespace.EXTERNAL_PLUGIN_NAMESPACE, filter)
				.findAny();

			if (!optCap.isPresent())
				return Result.err("no such plugin %s for type %s", pluginName, c.getName());

			Capability cap = optCap.get();

			Result<File, String> bundle = workspace.getBundle(cap.getResource());
			if (bundle.isErr())
				return bundle.asError();

			Object object = cap.getAttributes()
				.get("implementation");
			if (object == null || !(object instanceof String))
				return Result.err("no proper class attribute in plugin capability %s is %s", pluginName, object);

			String className = (String) object;

			URL url = bundle.unwrap()
				.toURI()
				.toURL();

			try (URLClassLoader cl = new URLClassLoader(new URL[] {
				url
			}, WorkspaceExternalPluginHandler.class.getClassLoader())) {
				@SuppressWarnings("unchecked")
				Class<?> impl = cl.loadClass(className);
				T instance = c.cast(impl.newInstance());
				try {
					return f.apply(instance);
				} catch (Exception e) {
					return Result.err("external plugin '%s' failed with: %s", pluginName, Exceptions.causes(e));
				}
			}
		} catch (ClassNotFoundException e) {
			return Result.err("no such class %s in %s for plugin %s", e.getMessage(), pluginName);
		} catch (Exception e) {
			return Result.err("could not instantiate class %s in %s for plugin %s: %s", e.getMessage(), pluginName,
				Exceptions.causes(e));
		}
	}

	@Override
	public void close() {}
}
