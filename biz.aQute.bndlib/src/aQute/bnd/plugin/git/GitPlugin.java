package aQute.bnd.plugin.git;

import java.io.*;
import java.util.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.lifecycle.*;
import aQute.lib.io.*;

/**
 * Adds .gitignore files to projects when created.
 */
@BndPlugin(name = "git")
public class GitPlugin extends LifeCyclePlugin {

	private static final String	GITIGNORE	= ".gitignore";

	@Override
	public void created(Project p) throws Exception {
		Formatter f = new Formatter();
		f.format("/%s/\n", p.getProperty(Constants.DEFAULT_PROP_TARGET_DIR, "generated"));
		f.format("/%s/\n", p.getProperty(Constants.DEFAULT_PROP_BIN_DIR, "bin"));
		f.format("/%s/\n", p.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR, "bin_test"));

		IO.store(f.toString(), p.getFile(GITIGNORE));
		f.close();

		//
		// Add some .gitignore to keep empty directories alive
		//
		for (File dir : p.getSourcePath()) {
			touch(dir);
		}

		touch(p.getTestSrc());
	}

	private void touch(File dir) throws IOException {
		if (!dir.isDirectory())
			dir.mkdirs();
		IO.store("", new File(dir, GITIGNORE));
	}

	@Override
	public String toString() {
		return "GitPlugin";
	}
}
