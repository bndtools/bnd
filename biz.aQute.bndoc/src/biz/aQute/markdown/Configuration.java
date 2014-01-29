package biz.aQute.markdown;

import java.io.*;

public interface Configuration {
	/**
	 * Header levels that should be numbered. If less than 1, no headers are
	 * numbered. The default is 2
	 * 
	 * @param level
	 * @return
	 */
	Configuration header_number(int level);

	int header_number();

	/**
	 * The actual file system directory where the resources are stored. Default
	 * is "www" int he current directory
	 */

	String resources_dir();

	Configuration resources_dir(File resources);

	Configuration resources_dir(String resources);

	/**
	 * The relative path to use on links to resources. The default is the name
	 * of the resource directory.
	 */
	String resources_relative();

	Configuration resources_relative(String resources);
}
