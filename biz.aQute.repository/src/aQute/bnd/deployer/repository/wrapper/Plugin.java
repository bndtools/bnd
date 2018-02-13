package aQute.bnd.deployer.repository.wrapper;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.repository.InfoRepository;
import aQute.lib.collections.MultiMap;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class Plugin extends BaseRepository
 implements aQute.bnd.service.Plugin, RegistryPlugin, Repository, Closeable {

	private Registry				registry;
	private Config					config;
	private Reporter				reporter	= new ReporterAdapter();
	private File					dir;
	private InfoRepositoryWrapper	wrapper;
	private boolean					init		= false;

	interface Config {
		String location();

		boolean reindex();

		String augments();
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public void setProperties(Map<String,String> map) throws Exception {
		config = Converter.cnv(Config.class, map);
		File file = IO.getFile(config.location());
		IO.mkdirs(file);
		if (!file.isDirectory()) {
			reporter.error("Repository Wrapper: cannot create cache: %s", file);
		}
		this.dir = file;
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	/**
	 * This is called when all initialization is done for the plugins, now we
	 * can obtain a list of appropriate repos.
	 */
	public void init() {
		if (init)
			return;

		init = true;

		try {

			//
			// Get the list if repos registered, repos that we can handle
			//

			List<InfoRepository> irs = new ArrayList<>();
			for (InfoRepository ir : registry.getPlugins(InfoRepository.class)) {
				irs.add(ir);
			}

			this.wrapper = new InfoRepositoryWrapper(dir, irs);

			if (config.reindex())
				this.wrapper.clear();

			// An augment file was specified, this specifies extra
			// reqs and caps for the analyzed files
			//

			if (config.augments() != null) {

				Workspace workspace = registry.getPlugin(Workspace.class);
				try (Processor p = new Processor(workspace)) {

					if (!config.augments().equals("WORKSPACE")) {
						File f = IO.getFile(workspace.getBuildDir(), config.augments());
						if (!f.isFile()) {
							if (reporter != null)
								reporter.error("No augment file found at %s", f.getAbsolutePath());
							return;
						}

						//
						// We read this in a processor that extends the
						// workspace so
						// we
						// can use workspace properties
						//

						p.setProperties(f);
						this.wrapper.clear(f.lastModified());
					}

					//
					// And then add it to the indexer to use.
					//

					this.wrapper.addAugment(p.getFlattenedProperties());
					this.wrapper.clear(workspace.getPropertiesFile().lastModified());
				}
			}

		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public void close() throws java.io.IOException {
		if (this.wrapper == null) {
			return;
		}
		this.wrapper.close();
	}

	FilterParser fp = new FilterParser();

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		init();
		MultiMap<Requirement,Capability> result = new MultiMap<>();
		try {
			wrapper.findProviders(result, requirements);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return (Map) result;
	}

	public String toString() {
		return wrapper != null ? wrapper.toString() : "<wrapper not set>";
	}

}
