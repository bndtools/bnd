package aQute.bnd.main;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.lib.getopt.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;

public class PatchCommand {
	bnd	bnd;

	public PatchCommand(bnd bnd) {
		this.bnd = bnd;
	}

	@Description("WIP")
	@Arguments(arg={"<older>","<newer>", "<patch>"})
	interface createOptions extends Options {
		
	}
	public void _create(createOptions opts) throws Exception {
		List<String> arguments = opts._();
		
		Jar a = new Jar(bnd.getFile(arguments.remove(0)));
		Manifest am = a.getManifest();
		
		Jar b = new Jar(bnd.getFile(arguments.remove(0)));
		Manifest bm = b.getManifest();

		File patch = bnd.getFile(arguments.remove(0));

		// TODO check arguments
		
		Set<String> delete = Create.set();

		for (String path : a.getResources().keySet()) {
			Resource br = b.getResource(path);
			if (br == null) {
				bnd.trace("DELETE    %s", path);
				delete.add(path);
			} else {
				Resource ar = a.getResource(path);
				if (isEqual(ar, br)) {
					bnd.trace("UNCHANGED %s", path);
					b.remove(path);
				} else
					bnd.trace("UPDATE    %s", path);
			}
		}

		bm.getMainAttributes().putValue("Patch-Delete", Processor.join(delete, ", "));
		bm.getMainAttributes().putValue("Patch-Version",
				am.getMainAttributes().getValue("Bundle-Version"));

		b.write(patch);
		a.close();
		a.close();
		
		// TODO proper close
	}

	private boolean isEqual(Resource ar, Resource br) throws Exception {
		InputStream ain = ar.openInputStream();
		try {
			InputStream bin = br.openInputStream();
			try {
				while (true) {
					int an = ain.read();
					int bn = bin.read();
					if (an == bn) {
						if (an == -1)
							return true;
					} else
						return false;
				}
			} finally {
				bin.close();
			}
		} finally {
			ain.close();
		}
	}

	@Description("WIP")
	@Arguments(arg={"<older>","<newer>", "<patch>"})
	interface applyOptions {
		
	}
	public void _apply(applyOptions opts) throws Exception {
//		List<String> arguments = opts._();
//		Jar a = new Jar(bnd.getFile(arguments.remove(0)));
//		Manifest am = a.getManifest();
//		
//		Jar b = new Jar(bnd.getFile(arguments.remove(0)));
//		Manifest bm = b.getManifest();
//
//		File patch = bnd.getFile(arguments.remove(0));
//		Jar a = new Jar(new File(old));
//		Jar b = new Jar(new File(patch));
//		Manifest bm = b.getManifest();
//
//		String patchDelete = bm.getMainAttributes().getValue("Patch-Delete");
//		String patchVersion = bm.getMainAttributes().getValue("Patch-Version");
//		if (patchVersion == null) {
//			error("To patch, you must provide a patch bundle.\nThe given " + patch
//					+ " bundle does not contain the Patch-Version header");
//			return;
//		}
//
//		Collection<String> delete = split(patchDelete);
//		Set<String> paths = new HashSet<String>(a.getResources().keySet());
//		paths.removeAll(delete);
//
//		for (String path : paths) {
//			Resource br = b.getResource(path);
//			if (br == null)
//				b.putResource(path, a.getResource(path));
//		}
//
//		bm.getMainAttributes().putValue("Bundle-Version", patchVersion);
//		b.write(new File(newer));
//		a.close();
//		b.close();
	}

}
