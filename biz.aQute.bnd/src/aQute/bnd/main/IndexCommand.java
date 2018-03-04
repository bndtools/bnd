package aQute.bnd.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.SimpleIndexer;
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
		File repositoryFile = opts.repositoryIndex()
			.getAbsoluteFile();
		if (repositoryFile == null) {
			repositoryFile = new File(outputDir, "").getAbsoluteFile();
		}
		boolean compress = false;
		if (repositoryFile.getName()
			.endsWith(".gz")) {
			compress = true;
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

		List<File> files = opts._arguments()
			.stream()
			.map(arg -> getFile(arg).getAbsoluteFile())
			.collect(Collectors.toList());

		if (files.isEmpty()) {
			bnd.out.println("argument <bundles..> did not contain any bundle files");
			return;
		}

		Map<String, String> config = new HashMap<>();

		try (OutputStream out = new FileOutputStream(repositoryFile)) {
			SimpleIndexer.index(files, out, base, compress, name);
		}
	}

}
