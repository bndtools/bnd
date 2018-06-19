package aQute.bnd.plugin.git;

import java.io.File;
import java.io.IOException;

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
		String gitignore = "";
		File file = p.getFile(GITIGNORE);
		if (file.isFile())
			gitignore = IO.collect(file);

		gitignore = addIgnoreDirectory(gitignore, p.getProperty(Constants.DEFAULT_PROP_TARGET_DIR, "generated"));
		gitignore = addIgnoreDirectory(gitignore, p.getProperty(Constants.DEFAULT_PROP_BIN_DIR, "bin"));
		gitignore = addIgnoreDirectory(gitignore, p.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR, "bin_test"));

		IO.store(gitignore, file);

		traverse(p.getBase());
	}

	private void traverse(File base) throws IOException {
		boolean foundsomething = false;
		for (File f : base.listFiles()) {
			if (f.isDirectory())
				traverse(f);

			foundsomething = true;
		}

		if (!foundsomething)
			addGitIgnore(base);
	}

	private String addIgnoreDirectory(String content, String ignore) {
		if (content.indexOf(ignore + "/") >= 0) {
			return content;
		}
		return content + "\n" + ignore + "/";
	}

	private void addGitIgnore(File dir) throws IOException {
		IO.mkdirs(dir);
		IO.store("", new File(dir, GITIGNORE));
	}

	@Override
	public String toString() {
		return "GitPlugin";
	}
}
