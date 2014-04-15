package aQute.bnd.deployer.repository.wrapper;

import java.io.*;
import java.util.*;

import org.osgi.resource.*;
import org.osgi.service.repository.*;

import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.*;
import aQute.bnd.service.repository.*;
import aQute.lib.collections.*;
import aQute.lib.converter.*;
import aQute.lib.io.*;
import aQute.lib.persistentmap.*;
import aQute.libg.reporter.*;
import aQute.service.reporter.*;

public class Plugin implements aQute.bnd.service.Plugin, RegistryPlugin, RegistryDonePlugin, Repository {

	private Registry							registry;
	private final List<InfoRepositoryWrapper>	wrappers	= new ArrayList<InfoRepositoryWrapper>();
	private PersistentMap<PersistentResource>	pmap;
	private Config								config;
	private Reporter							reporter	= new ReporterAdapter();

	interface Config {
		String location();
		boolean reindex();
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public void setProperties(Map<String,String> map) throws Exception {
		config = Converter.cnv(Config.class, map);
		File file = IO.getFile(config.location());
		file.mkdirs();
		if (!file.isDirectory()) {
			reporter.error("Repository Wrapper: cannot create cache: %s", file);
		}

		pmap = new PersistentMap<PersistentResource>(file, PersistentResource.class);
		if ( config.reindex())
			pmap.clear();
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public void done() throws IOException {
		for (InfoRepository ir : registry.getPlugins(InfoRepository.class)) {
			wrappers.add(new InfoRepositoryWrapper(ir, pmap));
		}
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		MultiMap<Requirement,Capability> result = new MultiMap<Requirement,Capability>();
		for (InfoRepositoryWrapper wrapper : wrappers) {
			wrapper.findProviders(result, requirements);
		}
		return (Map) result;
	}

}
