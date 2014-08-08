package aQute.bnd.plugin.ant;

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
@BndPlugin(name="ant")
public class AntPlugin extends LifeCyclePlugin {
	static String	DEFAULT	= "<?xml version='1.0' encoding='UTF-8'?>\n" + //
									"<project name='project' default='build'>\n" + //
									"        <import file='../cnf/build.xml' />\n" //
									+ "</project>\n";

	@Override
	public void created(Project p) throws IOException {
		Workspace workspace = p.getWorkspace();
		File source = workspace.getFile("ant/project.xml");
		File dest = p.getFile("build.xml");

		if (source.isFile())
			IO.copy(source, dest);
		else
			IO.store(DEFAULT, dest);
	}

	@Override
	public String toString() {
		return "AntPlugin";
	}

}
