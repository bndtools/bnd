package aQute.bnd.plugin.eclipse;

import java.io.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.build.*;
import aQute.bnd.service.lifecycle.*;
import aQute.lib.io.*;

/**
 * This plugin creates a build.xml file in the project when a project gets
 * created. You can either store a template under cnf/ant/project.xml or a
 * default is taken.
 */
@BndPlugin(name="eclipse")
public class EclipsePlugin extends LifeCyclePlugin {
	@Override
	public void created(Project p) throws IOException {
		Workspace workspace = p.getWorkspace();

		copy("project", ".project", p);
		copy("classpath", ".classpath", p);
	}

	private void copy(String source, String dest, Project p) throws IOException {
		File d = p.getFile(dest);
		if ( d.isFile()) {
			return;
		}

		File f = p.getWorkspace().getFile("eclipse/" + source + ".tmpl");
		InputStream in;

		if (f.isFile()) {
			in = new FileInputStream(f);
		} else {
			in = EclipsePlugin.class.getResourceAsStream(source);
			if (in == null) {
				p.error("Cannot find Eclipse default for %s", source);
				return;
			}
		}

		String s = IO.collect(in);
		String process = p.getReplacer().process(s);
		
		d.getParentFile().mkdirs();
		IO.store( process, d);
	}
	
	@Override
	public String toString() {
		return "EclipsePlugin";
	}

	@Override 
	public void init(Workspace ws) throws Exception {
		
		Project p = new Project(ws,ws.getFile("cnf"));
		created(p);
		
		for ( Project pp : ws.getAllProjects()) {
			created(pp);
		}
	}
	

}
