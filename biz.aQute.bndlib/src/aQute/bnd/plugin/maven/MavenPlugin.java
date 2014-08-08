package aQute.bnd.plugin.maven;

import java.io.*;
import java.util.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.build.*;
import aQute.bnd.service.lifecycle.*;
import aQute.lib.io.*;

/**
 * This plugin provides life cycle support for maven projects
 */

@Plugin(name="maven")
public class MavenPlugin extends LifeCyclePlugin {

	@Override
	public void created(Project p) throws IOException {
		Workspace workspace = p.getWorkspace();

		copy("pom.xml", "pom.xml", p);

		String rootPom = IO.collect(workspace.getFile("pom.xml"));
		rootPom.replaceAll("<!-- DO NOT EDIT MANAGED BY BND MAVEN LIFECYCLE PLUGIN -->\n*", "$0\n\t\t<module>" + p
				+ "</module>\n");
	}

	private void copy(String source, String dest, Project p) throws IOException {

		File f = p.getWorkspace().getFile("maven/" + source + ".tmpl");
		InputStream in;

		if (f.isFile()) {
			in = new FileInputStream(f);
		} else {
			in = MavenPlugin.class.getResourceAsStream(source);
			if (in == null) {
				p.error("Cannot find Maven default for %s", source);
				return;
			}
		}

		String s = IO.collect(in);
		String process = p.getReplacer().process(s);

		File d = p.getFile(dest);
		d.getParentFile().mkdirs();
		IO.store(process, d);
	}

	@Override
	protected String getPluginSetup() throws Exception {
		String s = super.getPluginSetup();

		Formatter f = new Formatter(s);
		try {
			f.format("\n#\n# Change disk layout to fit maven\n#\n\n");
			f.format("-outputmask = ${@bsn}-${version;===S;${@version}}.jar\n");
			f.format("src=src/main/java\n");
			f.format("bin=target/classes\n");
			f.format("testsrc=src/test/java\n");
			f.format("testbin=target/test-classes\n");
			f.format("target-dir=target\n");
			return f.toString();
		}
		finally {
			f.close();
		}
	}

	@Override
	public String toString() {
		return "MavenPlugin";
	}
}
