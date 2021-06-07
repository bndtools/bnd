package org.bndtools.api;

import aQute.bnd.service.RepositoryPlugin;

/**
 * This interface is used with {@link RepositoryPlugin} repositories. It signals
 * to the Repository View to not display the repository when it is empty. Such
 * repositories are populated automatically by Eclipse workspace conditions. For
 * example, when Maven projects are in the Eclipse workspace.
 */
public interface PopulatedRepository {

	boolean isEmpty();

}
