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

@BndPlugin(name="maven")
public class MavenPlugin extends LifeCyclePlugin {

	@Override
	public void created(Project p) throws IOException {
		Workspace workspace = p.getWorkspace();

		copy("pom.xml", "pom.xml", p);

		File root = workspace.getFile("pom.xml");
		
		doRoot(p, root);
		
		String rootPom = IO.collect(root);
		if ( !rootPom.contains(getTag(p))) {
			rootPom = rootPom.replaceAll("<!-- DO NOT EDIT MANAGED BY BND MAVEN LIFECYCLE PLUGIN -->\n", 
					"$0\n\t\t"+ getTag(p) + "\n");
			IO.store(rootPom, root);
		}
	}

	private void doRoot(Project p, File root) throws IOException {
		if( !root.isFile()) {
			IO.delete(root);
			copy("rootpom.xml", "../pom.xml", p);
		}
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
	public String augmentSetup(String setup, String alias, Map<String,String> parameters) throws Exception {
		Formatter f = new Formatter();
		f.format("%s", setup);
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
	public void delete(Project p) throws IOException {
		File root = p.getWorkspace().getFile("pom.xml");
		String rootPom = IO.collect(root);
		if ( rootPom.contains(getTag(p))) {
			rootPom = rootPom.replaceAll("\n\\s*" + getTag(p) + "\\s*", "\n");
			IO.store(rootPom, root);
		}
		
	}

	private String getTag(Project p) {
		return "<module>"+p+"</module>";
	}
	
	
	@Override
	public String toString() {
		return "MavenPlugin";
	}
}
