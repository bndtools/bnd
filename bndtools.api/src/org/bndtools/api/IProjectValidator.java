package org.bndtools.api;

import aQute.bnd.build.Project;

public interface IProjectValidator {
    /**
     * Validate a project, errors are reported the bnd way.
     *
     * @param project
     * @throws Exception
     */
    void validateProject(Project project) throws Exception;
}
