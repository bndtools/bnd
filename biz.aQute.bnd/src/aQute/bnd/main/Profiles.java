package aQute.bnd.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Parameters;
import aQute.bnd.main.bnd.ProfileOptions;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.Version;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;

public class Profiles extends Processor {
	private final static Logger	logger	= LoggerFactory.getLogger(Profiles.class);
	private bnd					bnd;

	// private ProfileOptions options;

	public Profiles(bnd bnd, ProfileOptions options) {
		super(bnd);
		getSettings(bnd);
		this.bnd = bnd;
		// this.options = options;
	}

	public interface CreateOptions extends Options {
		String[] properties();

		String bsn();

		Version version();

		Instructions match();

		String output();

		Glob extension();
	}

	public void _create(CreateOptions options) throws Exception {
		Builder b = new Builder();
		bnd.addClose(b);

		b.setBase(bnd.getBase());

		if (options.properties() != null) {
			for (String propertyFile : options.properties()) {
				File pf = bnd.getFile(propertyFile);
				b.addProperties(pf);
			}
		}

		if (options.bsn() != null)
			b.setProperty(Constants.BUNDLE_SYMBOLICNAME, options.bsn());

		if (options.version() != null)
			b.setProperty(Constants.BUNDLE_VERSION, options.version()
				.toString());

		Instructions match = options.match();

		Parameters packages = new Parameters();
		Parameters capabilities = new Parameters();

		Collection<String> paths = new ArrayList<>(new Parameters(b.getProperty("-paths"), bnd).keySet());
		if (paths.isEmpty())
			paths = options._arguments();

		logger.debug("input {}", paths);

		ResourceBuilder pb = new ResourceBuilder();

		for (String root : paths) {
			File f = bnd.getFile(root);
			if (!f.exists()) {
				error("could not find %s", f);
			} else {

				Glob g = options.extension();
				if (g == null)
					g = new Glob("*.jar");

				Collection<File> files = IO.tree(f, "*.jar");
				logger.debug("will profile {}", files);

				for (File file : files) {
					Domain domain = Domain.domain(file);
					if (domain == null) {
						error("Not a bundle because no manifest %s", file);
						continue;
					}

					String bsn = domain.getBundleSymbolicName()
						.getKey();
					if (bsn == null) {
						error("Not a bundle because no manifest %s", file);
						continue;
					}

					if (match != null) {
						Instruction instr = match.finder(bsn);
						if (instr == null || instr.isNegated()) {
							logger.debug("skipped {} because of non matching bsn {}", file, bsn);
							continue;
						}
					}

					Parameters eps = domain.getExportPackage();
					Parameters pcs = domain.getProvideCapability();

					logger.debug("parse {}:\ncaps: {}\npac: {}\n", file, pcs, eps);

					packages.mergeWith(eps, false);
					capabilities.mergeWith(pcs, false);

				}

			}
		}

		b.setProperty(Constants.PROVIDE_CAPABILITY, capabilities.toString());
		b.setProperty(Constants.EXPORT_PACKAGE, packages.toString());
		logger.debug("Found {} packages and {} capabilities", packages.size(), capabilities.size());
		Jar jar = b.build();
		File f = b.getOutputFile(options.output());
		logger.debug("Saving as {}", f);
		jar.write(f);
	}

}
