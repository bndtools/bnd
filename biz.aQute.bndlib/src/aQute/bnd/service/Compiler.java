package aQute.bnd.service;

import java.io.File;
import java.util.Collection;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;

public interface Compiler {
	boolean compile(Project project, Collection<File> sources, Collection<Container> buildpath, File bin)
		throws Exception;
}
