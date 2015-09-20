package org.bndtools.templating;

import java.io.IOException;

public interface Template extends Comparable<Template> {

	String getName();

	String getCategory();

	int getRanking();
	
	ResourceMap getInputSources() throws IOException;

}
