package aQute.bnd.build;

import java.io.*;
import java.util.*;

import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.libg.reporter.*;

public class Deployer {

	private final Processor			processor;
	private final ProjectMessages	msgs;

	public Deployer(Processor processor) {
		this.processor = processor;
		this.msgs = ReporterMessages.base(this.processor, ProjectMessages.class);
	}

	/**
	 * Deploy the file (which must be a bundle) into the repository.
	 *
	 * @param name
	 *            The repository name
	 * @param file
	 *            bundle
	 */
	public void deploy(String name, File file) throws Exception {
		List<RepositoryPlugin> plugins = processor.getPlugins(RepositoryPlugin.class);

		RepositoryPlugin rp = null;
		for (RepositoryPlugin plugin : plugins) {
			if (!plugin.canWrite()) {
				continue;
			}
			if (name == null) {
				rp = plugin;
				break;
			} else if (name.equals(plugin.getName())) {
				rp = plugin;
				break;
			}
		}

		if (rp != null) {
			try {
				rp.put(new BufferedInputStream(new FileInputStream(file)), new RepositoryPlugin.PutOptions());
				return;
			}
			catch (Exception e) {
				msgs.DeployingFile_On_Exception_(file, rp.getName(), e);
			}
			return;
		}
		processor.trace("No repo found " + file);
		throw new IllegalArgumentException("No repository found for " + file);
	}

	/**
	 * Deploy the file (which must be a bundle) into the repository.
	 *
	 * @param file
	 *            bundle
	 */
	public void deploy(File file) throws Exception {
		String name = processor.getProperty(Constants.DEPLOYREPO);
		deploy(name, file);
	}

}
