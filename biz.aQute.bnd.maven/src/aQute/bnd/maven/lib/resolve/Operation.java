package aQute.bnd.maven.lib.resolve;

import java.io.File;

import biz.aQute.resolve.Bndrun;

@FunctionalInterface
public interface Operation {
	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param bndrunFile the bndrun file to operate on
	 * @param taskname name of the task performing the operation
	 * @param bndrun the {@code Bndrun} instance created for the operation
	 * @return the number of errors
	 * @throws Exception
	 */
	int apply(File bndrunFile, String taskname, Bndrun bndrun) throws Exception;

}
