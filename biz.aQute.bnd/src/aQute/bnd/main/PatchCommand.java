package aQute.bnd.main;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.libg.generics.Create;

public class PatchCommand {
	private final static Logger	logger	= LoggerFactory.getLogger(PatchCommand.class);
	bnd							bnd;

	public PatchCommand(bnd bnd) {
		this.bnd = bnd;
	}

	@Description("WIP")
	@Arguments(arg = {
		"<older>", "<newer>", "<patch>"
	})
	interface createOptions extends Options {

	}

	public void _create(createOptions opts) throws Exception {
		List<String> arguments = opts._arguments();

		Jar a = new Jar(bnd.getFile(arguments.remove(0)));
		Manifest am = a.getManifest();

		Jar b = new Jar(bnd.getFile(arguments.remove(0)));
		Manifest bm = b.getManifest();

		File patch = bnd.getFile(arguments.remove(0));

		// TODO check arguments

		Set<String> delete = Create.set();

		for (String path : a.getResources()
			.keySet()) {
			Resource br = b.getResource(path);
			if (br == null) {
				logger.debug("DELETE    {}", path);
				delete.add(path);
			} else {
				Resource ar = a.getResource(path);
				if (isEqual(ar, br)) {
					logger.debug("UNCHANGED {}", path);
					b.remove(path);
				} else
					logger.debug("UPDATE    {}", path);
			}
		}

		bm.getMainAttributes()
			.putValue("Patch-Delete", Processor.join(delete, ", "));
		bm.getMainAttributes()
			.putValue("Patch-Version", am.getMainAttributes()
				.getValue(Constants.BUNDLE_VERSION));

		b.write(patch);
		a.close();
		b.close();

		// TODO proper close
	}

	private boolean isEqual(Resource ar, Resource br) throws Exception {
		try (InputStream ain = ar.openInputStream(); InputStream bin = br.openInputStream()) {
			while (true) {
				int an = ain.read();
				int bn = bin.read();
				if (an == bn) {
					if (an == -1)
						return true;
				} else
					return false;
			}
		}
	}

	@Description("WIP")
	@Arguments(arg = {
		"<older>", "<newer>", "<patch>"
	})
	interface applyOptions {

	}

	public void _apply(@SuppressWarnings("unused") applyOptions opts) throws Exception {
		// List<String> arguments = opts._();
		// Jar a = new Jar(bnd.getFile(arguments.remove(0)));
		// Manifest am = a.getManifest();
		//
		// Jar b = new Jar(bnd.getFile(arguments.remove(0)));
		// Manifest bm = b.getManifest();
		//
		// File patch = bnd.getFile(arguments.remove(0));
		// Jar a = new Jar(new File(old));
		// Jar b = new Jar(new File(patch));
		// Manifest bm = b.getManifest();
		//
		// String patchDelete = bm.getMainAttributes().getValue("Patch-Delete");
		// String patchVersion =
		// bm.getMainAttributes().getValue("Patch-Version");
		// if (patchVersion == null) {
		// error("To patch, you must provide a patch bundle.\nThe given " +
		// patch
		// + " bundle does not contain the Patch-Version header");
		// return;
		// }
		//
		// Collection<String> delete = split(patchDelete);
		// Set<String> paths = new HashSet<String>(a.getResources().keySet());
		// paths.removeAll(delete);
		//
		// for (String path : paths) {
		// Resource br = b.getResource(path);
		// if (br == null)
		// b.putResource(path, a.getResource(path));
		// }
		//
		// bm.getMainAttributes().putValue(Constants.BUNDLE_VERSION,
		// patchVersion);
		// b.write(new File(newer));
		// a.close();
		// b.close();
	}

}
