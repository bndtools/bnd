package aQute.bnd.main;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import aQute.bnd.main.bnd.verboseOptions;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.io.FileTree;
import aQute.lib.io.IO;

public class IndexCommand extends Processor {

	public static final String	DEFAULT_INDEX_FILE			= "index.xml";

	public static final String	COMPRESSED_FILE_EXTENSION	= ".gz";

	final bnd					bnd;

	public IndexCommand(bnd bnd) throws Exception {
		this.bnd = bnd;
	}

	@Description("Index bundles from the local file system")
	@Arguments(arg = {
		"bundles..."
	})
	interface indexOptions extends verboseOptions {
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
			repositoryFile = new File(outputDir, DEFAULT_INDEX_FILE).getAbsoluteFile();
		} else {
			repositoryFile = repositoryFile.getAbsoluteFile();
		}
		boolean compress = false;
		if (repositoryFile.getName()
			.endsWith(COMPRESSED_FILE_EXTENSION)) {
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

		// using AntGlobs
		final List<File> files = new FileTree().getFiles(outputDir, opts._arguments());

		if (opts.verbose()) {
			bnd.out.println(String.format("Number of files to index %d:\n", files.size()));
			files.forEach(f -> bnd.out.println("" + f));
		}

		if (files.isEmpty()) {
			bnd.out.println("argument <bundles..> did not contain any bundle files");
			bnd.out.println(opts._command()
				.execute(bnd, "help", Collections.singletonList("index")));

			return;
		}

		new SimpleIndexer().reporter(bnd)
			.files(files)
			.base(base)
			.name(name)
			.compress(compress)
			.index(repositoryFile);
	}
}
