package org.bndtools.templating;

import java.io.IOException;
import java.io.InputStream;

public interface Template extends Comparable<Template> {

	String getName();

	String getCategory();

	int getRanking();
	
	ResourceMap getInputSources() throws IOException;
	
	InputStream getIconData() throws IOException;

}
