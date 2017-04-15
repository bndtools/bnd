package aQute.bnd.plugin.git;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.lifecycle.LifeCyclePlugin;
import aQute.lib.io.IO;

/**
 * Adds .gitignore files to projects when created.
 */
@BndPlugin(name = "git")
public class GitPlugin extends LifeCyclePlugin {

	private static final String GITIGNORE = ".gitignore";

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
		IO.mkdirs(dir);
		IO.store("", new File(dir, GITIGNORE));
	}

	@Override
	public String toString() {
		return "GitPlugin";
	}
}
