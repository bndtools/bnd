package org.bndtools.templating;

import java.io.IOException;
import java.net.URI;

import org.osgi.framework.Version;

public interface Template {

	/**
	 * The name of this template.
	 */
	String getName();

	/**
	 * A short description of the template that may be shown in a summary view of all
	 * templates. This should NOT be expensive to fetch.
	 */
	String getShortDescription();

	/**
	 * The category of the template. May be null, in which case the template will
	 * be considered uncategorised.
	 */
	String getCategory();

	/**
	 * The ranking of this template in relation to other templates of the same
	 * category. If you don't care, just return zero.
	 */
	int getRanking();
	
	/**
	 * The version of this template.
	 */
	Version getVersion();

	/**
	 * A map of the source files provided by the template.
	 * @throws IOException
	 */
	ResourceMap getInputSources() throws IOException;

	/**
	 * A URL to an icon for the template. May be null.
	 */
	URI getIcon();
	
	/**
	 * A URL to a help document for the template. May be null.
	 */
	URI getHelpContent();

}
