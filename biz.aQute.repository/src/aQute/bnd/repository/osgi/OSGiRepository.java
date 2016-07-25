package aQute.bnd.repository.osgi;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.Prepare;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.configurable.Config;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.service.reporter.Reporter;

@BndPlugin(name = "OSGiRepository", parameters = Config.class)
public class OSGiRepository implements Plugin, RepositoryPlugin, Actionable, Refreshable, RegistryPlugin, Prepare {
	final static int YEAR = 365 * 24 * 60 * 60;
	interface Config {
		/**
		 * A Comma separate list of URLs point to an OSGi Resource file.
		 */
		String locations();

		int max_stale(int n);

		String cache(String deflt);

		String name();
	}

	private Config		config;
	private OSGiIndex	index;
	private Reporter	reporter	= new Slf4jReporter(OSGiRepository.class);
	private Registry	registry;

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Read only");
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		File target = IO.getFile(getIndex().getCache(), bsn + "-" + version);

		Promise<File> promise = getIndex().get(bsn, version, target);
		if (promise == null)
			return null;

		if (listeners.length == 0) {
			return promise.getValue();
		}

		new DownloadListenerPromise(reporter, "Download " + bsn + "-" + version + " into " + config.name(), promise,
				listeners);

		return target;
	}

	private synchronized OSGiIndex getIndex() throws Exception {
		if (index != null)
			return index;
		return getIndex(false);
	}

	private synchronized OSGiIndex getIndex(boolean refresh) throws Exception {

		HttpClient client = registry.getPlugin(HttpClient.class);
		Workspace ws = registry.getPlugin(Workspace.class);
		File defltCache = ws.getFile("cnf/cache" + config.name());
		File cache = IO.getFile(defltCache, config.cache(config.name()));

		if (refresh)
			IO.delete(cache);

		List<String> strings = Strings.split(config.locations());
		List<URI> urls = new ArrayList<>(strings.size());
		for (String s : strings) {
			urls.add(new URI(s));
		}
		index = new OSGiIndex(config.name(), client, cache, urls, config.max_stale(YEAR));
		return index;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		return getIndex().getBridge().list(pattern);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		return getIndex().getBridge().versions(bsn);
	}

	@Override
	public String getName() {
		return config.name();
	}

	@Override
	public String getLocation() {
		return config.locations();
	}

	@Override
	public Map<String,Runnable> actions(final Object... target) throws Exception {
		Map<String,Runnable> menu = new LinkedHashMap<>();
		switch (target.length) {
			case 0 :
				menu.put("Reload Index & Bundles", new Runnable() {

					@Override
					public void run() {
						try {
							getIndex(true);
						} catch (Exception e) {
							throw Exceptions.duck(e);
						}
					}
				});
				break;
			case 2 :
				menu.put("Reload Bundle", new Runnable() {

					@Override
					public void run() {
						try {
							String bsn = (String) target[0];
							Version version = (Version) target[1];
							File f = get(bsn, version, null);
							if (f != null)
								IO.delete(f);
						} catch (Exception e) {
							throw Exceptions.duck(e);
						}
					}
				});
				break;
		}
		return menu;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		return getIndex().getBridge().tooltip(target);
	}

	@Override
	public String title(Object... target) throws Exception {
		return getIndex().getBridge().title(target);
	}

	@Override
	public synchronized boolean refresh() throws Exception {
		getIndex(false);
		return true;
	}

	@Override
	public File getRoot() throws Exception {
		return getIndex().getCache();
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {
		config = Converter.cnv(Config.class, map);
	}

	@Override
	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public void prepare() throws Exception {
		getIndex();
	}

	@Override
	public String toString() {
		return "OSGi[name=" + config.name() + "]";
	}
}
