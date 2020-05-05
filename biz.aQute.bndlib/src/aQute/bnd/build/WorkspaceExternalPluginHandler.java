package aQute.bnd.build;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.VersionRange;
import org.osgi.resource.Capability;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.MainClassNamespace;
import aQute.bnd.service.externalplugin.ExternalPluginNamespace;
import aQute.bnd.service.progress.ProgressPlugin.Task;
import aQute.bnd.service.progress.TaskManager;
import aQute.bnd.service.result.FunctionWithException;
import aQute.bnd.service.result.Result;
import aQute.bnd.version.MavenVersion;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.command.Command;

public class WorkspaceExternalPluginHandler implements AutoCloseable {

	final Workspace workspace;

	WorkspaceExternalPluginHandler(Workspace workspace) {
		this.workspace = workspace;
	}

	public <T, R> Result<R, String> call(String pluginName, Class<T> c, FunctionWithException<T, Result<R, String>> f) {
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

	public Result<String, String> call(String mainClass, VersionRange range, Processor context, String classpathSpec,
		String[] args) {
		List<File> cp = new ArrayList<>();
		try {

			String filter = MainClassNamespace.filter(mainClass, range);

			Optional<Capability> optCap = workspace.findProviders(MainClassNamespace.MAINCLASS_NAMESPACE, filter)
				.findAny();

			if (!optCap.isPresent())
				return Result.err("no such man class %s", mainClass);

			Capability cap = optCap.get();

			Result<File, String> bundle = workspace.getBundle(cap.getResource());
			if (bundle.isErr())
				return bundle.asError();

			cp.add(bundle.unwrap());

			Command c = new Command();

			c.setTrace();
			c.setCwd(context.getBase());
			c.setTimeout(1, TimeUnit.MINUTES);

			c.add(context.getProperty("java", IO.getJavaExecutablePath("java")));
			c.add("-cp");

			Parameters cpp = new Parameters(classpathSpec);

			for (Map.Entry<String, Attrs> e : cpp.entrySet()) {
				String v = e.getValue()
					.getVersion();
				MavenVersion mv = MavenVersion.parseMavenString(v);

				Result<File, String> result = workspace.getBundle(e.getKey(), mv.getOSGiVersion(), null);
				if (result.isErr())
					return result.asError();

				cp.add(result.unwrap());
			}

			String classpath = Strings.join(File.pathSeparator, cp);
			c.add(classpath);

			c.add(mainClass);
			for (String arg : args)
				c.add(arg);

			StringBuilder stdout = new StringBuilder();
			StringBuilder stderr = new StringBuilder();

			int exitCode = TaskManager.with(getTask(c), () -> c.execute(stdout, stderr));

			if (exitCode == 0)
				return Result.ok(stdout.toString());
			else
				return Result.err(stdout.toString() + "\n---\n" + stderr.toString());

		} catch (Exception e) {
			return Result.err("Failed with: %s", Exceptions.causes(e));
		}
	}

	private Task getTask(Command c) {
		return new Task() {

			private boolean canceled;

			@Override
			public void worked(int units) {}

			@Override
			public void done(String message, Throwable e) {}

			@Override
			public boolean isCanceled() {
				return canceled;
			}

			@Override
			public void abort() {
				this.canceled = true;
				c.cancel();
			}
		};
	}

	@Override
	public void close() {}

}
