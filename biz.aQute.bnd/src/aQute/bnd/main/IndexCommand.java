package aQute.bnd.main;

import java.io.File;
import java.net.URI;
import java.util.List;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;

public class IndexCommand extends Processor {
	final bnd bnd;

	public IndexCommand(bnd bnd) throws Exception {
		this.bnd = bnd;
	}

	@Description("Index bundles from the local file system")
	@Arguments(arg = {
		"bundles..."
	})
	interface indexOptions extends Options {
		@Description("The name of the repository index file (default: 'index.xml'). To enable GZIP compression use the file extension '.gz' (e.g. 'index.xml.gz')")
		File repositoryIndex();

		@Description("The directory to write the repository index file (default: the current directory)")
		File directory();

		@Description("URI from which to make paths in the index file relative (default: relative to the output file directory). The specified value must be a prefix of the absolute output file directory in order to have any effect")
		URI base();

		@Description("The name of the index (default: name of the output file directory)")
		String name();
	}

	public void _index(indexOptions opts) throws Exception {
		File outputDir = opts.directory();
		if (outputDir == null) {
			outputDir = IO.work;
		}
		File repositoryFile = opts.repositoryIndex();
		if (repositoryFile == null) {
			repositoryFile = new File(outputDir, "").getAbsoluteFile();
		}
		URI base = opts.base();
		if (base == null) {
			base = repositoryFile.getParentFile()
				.toURI();
		}
		String name = opts.name();
		if (name == null) {
			name = repositoryFile.getParentFile()
				.getName();
		}

		ResourcesRepository resourcesRepository = new ResourcesRepository();
		XMLResourceGenerator xmlResourceGenerator = new XMLResourceGenerator();

		List<String> args = opts._arguments();

		boolean anyBundlesFound = false;

		for (String arg : args) {
			File bundle = getFile(arg).getAbsoluteFile();
			if (bundle.isDirectory() || !bundle.canRead() || bundle.isHidden() || !bundle.exists()) {
				continue;
			}

			ResourceBuilder resourceBuilder = new ResourceBuilder();
			URI relative = base.relativize(bundle.toURI());
			if (resourceBuilder.addFile(bundle, relative)) {
				resourcesRepository.add(resourceBuilder.build());
				anyBundlesFound = true;
			}
		}

		if (!anyBundlesFound) {
			bnd.out.println("argument <bundles..> did not contain any bundle files");
			return;
		}

		IO.mkdirs(repositoryFile.getParentFile());
		xmlResourceGenerator.name(name)
			.repository(resourcesRepository)
			.save(repositoryFile);
	}

}
