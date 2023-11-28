package biz.aQute.bnd.proxy.generator;

import java.io.File;
import java.util.Optional;

import aQute.bnd.service.generate.Options;

/**
 * The options for the FacdaeGenerator command.
 */
public interface FacadeSourceGenOptions extends Options {
	/**
	 * Define the output file
	 *
	 * @return the output file or empty
	 */
	Optional<File> output();

	/**
	 * The class to extend. This is a util class that handles the environmental
	 * interface.
	 *
	 * @return the extend class
	 */
	String extend();

	/**
	 * The package the output classes should be defined in.
	 *
	 * @return the output package
	 */
	String package_();

}
