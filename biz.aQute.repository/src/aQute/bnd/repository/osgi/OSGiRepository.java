package aQute.bnd.repository.osgi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.Prepare;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.configurable.Config;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;

@BndPlugin(name = "OSGiRepository", parameters = Config.class)
public class OSGiRepository extends BaseRepository
	implements Plugin, RepositoryPlugin, Actionable, Refreshable, RegistryPlugin, Prepare, Closeable {
	private final static Logger	logger				= LoggerFactory.getLogger(OSGiRepository.class);
	final static int			YEAR				= 365 * 24 * 60 * 60;
	static int					DEFAULT_POLL_TIME	= (int) TimeUnit.MINUTES.toSeconds(5);

	interface Config {
		/**
		 * A Comma separate list of URLs point to an OSGi Resource file.
		 */
		String locations();

		int max_stale(int n);

		String cache();

		String name();

		int poll_time(int pollTimeInSecs);
	}

	private Config				config;
	private OSGiIndex			index;
	private Reporter			reporter;
	private Registry			registry;
	private ScheduledFuture<?>	poller;
	private volatile boolean	stale	= false;
	private final AtomicBoolean	inPoll	= new AtomicBoolean();

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Read only");
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		File target = IO.getFile(getIndex().getCache(), bsn + "-" + version + ".jar");

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
		if (index != null) {
			return index;
		}
		return getIndex(false);
	}

	synchronized OSGiIndex getIndex(boolean refresh) throws Exception {

		HttpClient client = registry.getPlugin(HttpClient.class);

		Workspace ws = registry.getPlugin(Workspace.class);
		if (ws == null) {
			ws = Workspace.createDefaultWorkspace();
		}
		String cachePath = config.cache();
		File cache = (cachePath == null) ? ws.getCache(config.name()) : ws.getFile(cachePath);

		if (refresh) {
			IO.delete(cache);
		}

		List<String> strings = Strings.split(config.locations());
		List<URI> urls = new ArrayList<>(strings.size());
		for (String s : strings) {
			urls.add(new URI(s));
		}

		if (poller == null) {
			Parameters gestalt = ws.getGestalt();
			if (!(gestalt.containsKey(Constants.GESTALT_BATCH) || gestalt.containsKey(Constants.GESTALT_CI)
				|| gestalt.containsKey(Constants.GESTALT_OFFLINE))) {
				int polltime = config.poll_time(DEFAULT_POLL_TIME);
				if (polltime > 0) {
					poller = Processor.getScheduledExecutor()
						.scheduleAtFixedRate(this::pollTask, polltime, polltime, TimeUnit.SECONDS);
				}
			}
		}
		index = new OSGiIndex(config.name(), client, cache, urls, config.max_stale(YEAR), refresh);
		if (refresh) {
			index.getBridgeRepository()
				.onResolve(this::notifyRepositoryListeners);
		}
		stale = false;
		return index;
	}

	/**
	 * Notify all {@link RepositoryListenerPlugin}s that this repository is
	 * updated.
	 */
	private void notifyRepositoryListeners() {
		registry.getPlugins(RepositoryListenerPlugin.class)
			.forEach((RepositoryListenerPlugin rp) -> rp.repositoryRefreshed(this));
	}

	private void pollTask() {
		if (inPoll.getAndSet(true))
			return;
		try {
			poll();
		} catch (Exception e) {
			logger.debug("During polling", e);
		} finally {
			inPoll.set(false);
		}
	}

	void poll() throws Exception {
		if (stale) {
			return;
		}
		OSGiIndex ix;
		synchronized (this) {
			ix = index;
		}
		if (ix == null)
			return;
		if (ix.isStale()) {
			stale = true;
			this.notifyRepositoryListeners();
		}
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		return getIndex().getBridge()
			.list(pattern);
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		return getIndex().getBridge()
			.versions(bsn);
	}

	@Override
	public String getName() {
		return config.name();
	}

	@Override
	public String getLocation() {
		try {
			return Strings.join(getIndex().getURIs());
		} catch (Exception e) {
			return config.locations();
		}
	}

	@Override
	public Map<String, Runnable> actions(final Object... target) throws Exception {
		Map<String, Runnable> menu = new LinkedHashMap<>();
		switch (target.length) {
			case 0 :
				menu.put("Reload Index & Bundles", () -> {
					try {
						getIndex(true);
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
				});
				break;
			case 2 :
				menu.put("Reload Bundle", () -> {
					try {
						String bsn = (String) target[0];
						Version version = (Version) target[1];
						File f = get(bsn, version, null);
						if (f != null)
							IO.delete(f);
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
				});
				break;
		}
		return menu;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (target.length == 0) {
			try (Formatter f = new Formatter()) {
				if (stale) {
					f.format("[stale] Needs reload, see menu\n");
				}
				f.format("Name             : %s\n", getName());
				f.format("Cache            : %s\n", getRoot());
				f.format("Max stale (secs) : %s\n", config.max_stale(YEAR));
				f.format("\n" + "URLs            :\n");
				for (URI uri : getIndex().getURIs()) {
					f.format("    %s\n", uri);
				}
				return f.toString();
			}
		}
		return getIndex().getBridge()
			.tooltip(target);
	}

	@Override
	public String title(Object... target) throws Exception {
		if (target.length == 0 && stale) {
			return getName() + " [stale]";
		}
		return getIndex().getBridge()
			.title(target);
	}

	@Override
	public synchronized boolean refresh() {
		return refresh(false);
	}

	boolean refresh(boolean b) {
		try {
			getIndex(b);
			return true;
		} catch (Exception e) {
			logger.error("Refreshing repository {} failed", this.getName(), e);
			return false;
		}
	}

	@Override
	public File getRoot() throws Exception {
		return getIndex().getCache();
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
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
		String location;
		try {
			location = getRoot().getAbsolutePath();
		} catch (Exception e) {
			location = config.cache();
		}
		return String.format("%s [%-40s r/w=%s]", getName(), location, canWrite());
	}

	@Override
	public void close() throws IOException {
		ScheduledFuture<?> p;
		synchronized (this) {
			p = poller;
		}
		if (p != null)
			p.cancel(true);
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		try {
			return getIndex().findProviders(requirements);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
