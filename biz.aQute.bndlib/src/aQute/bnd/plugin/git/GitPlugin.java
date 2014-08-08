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
@BndPlugin(name="git")
public class GitPlugin extends LifeCyclePlugin {

	@Override
	public void created(Project p) throws IOException {
		Formatter f = new Formatter();
		f.format("/%s/\n", p.getProperty(Constants.DEFAULT_PROP_TARGET_DIR, "generated" ));
		f.format("/%s/\n", p.getProperty(Constants.DEFAULT_PROP_BIN_DIR, "bin" ));
		f.format("/%s/\n", p.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR, "bin_test" ));
		
		IO.store(f.toString(), p.getFile(".gitignore"));
		
		//
		// Add some .gitignore to keep empty directories alive
		//
		
		IO.store("", IO.getFile( p.getSrc(), ".gitignore"));
		IO.store("", IO.getFile( p.getTestSrc(), ".gitignore"));
		
		f.close();
	}

	@Override
	public String toString() {
		return "GitPlugin";
	}
}
