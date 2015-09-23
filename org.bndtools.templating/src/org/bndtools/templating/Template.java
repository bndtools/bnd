package org.bndtools.templating;

import java.io.IOException;
import java.net.URI;

public interface Template extends Comparable<Template> {

	String getName();

	String getCategory();

	int getRanking();

	ResourceMap getInputSources() throws IOException;

	URI getIcon();
	
	URI getDescriptionText();

}
