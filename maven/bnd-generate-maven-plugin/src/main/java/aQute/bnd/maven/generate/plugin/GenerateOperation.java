package aQute.bnd.maven.generate.plugin;

import aQute.bnd.build.Project;

@FunctionalInterface
public interface GenerateOperation {
	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param taskname name of the task performing the operation
	 * @param project the project to run the generate task on
	 * @return the number of errors
	 * @throws Exception
	 */
	int apply(String taskname, Project project) throws Exception;

}
