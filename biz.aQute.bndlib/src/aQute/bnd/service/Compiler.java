package aQute.bnd.service;

import java.io.*;
import java.util.*;

import aQute.bnd.build.*;

public interface Compiler {
	boolean compile(Project project, Collection<File> sources, Collection<Container> buildpath,
			File bin) throws Exception;
}
