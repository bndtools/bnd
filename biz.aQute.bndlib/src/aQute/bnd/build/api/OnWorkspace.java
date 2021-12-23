package aQute.bnd.build.api;

import java.util.Collection;
import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

/**
 * The Workspace can notify interested parties when certain events happen. These
 * events can be called multiple times since in an IDE, the workspace can change
 * its properties, its projects, and its repositories.
 * <p>
 * Callbacks will always happen on another thread and will not be called after
 * the close has returned.
 */
@ProviderType
public interface OnWorkspace extends AutoCloseable {

	/**
	 * Callback when the workspace is initializing. This callback will be always
	 * directly and when the properties of the workspace change.
	 *
	 * @param cb the callback, will be called at least once
	 */
	OnWorkspace initial(Consumer<? super Workspace> cb);

	/**
	 * The set of projects has changed for some reason
	 *
	 * @param projects the callback, will get the current set of projects
	 */
	OnWorkspace projects(Consumer<? super Collection<Project>> projects);

	/**
	 * Callback when the workspace has a message (error or warning)
	 *
	 * @param cb the callback to be called with a message
	 */
	OnWorkspace message(Consumer<? super Workspace> cb);

	/**
	 * Callback when the workspace is closing
	 *
	 * @param cb the callback called when the workspace closes.
	 */
	OnWorkspace closing(Consumer<? super Workspace> cb);

	/**
	 * Callback when the repositories have all loaded or failed after an
	 * initialize. This will always be called after an initial event.
	 *
	 * @param cb the callback called when the repositories are ready
	 */
	OnWorkspace repositoriesReady(Consumer<? super Collection<RepositoryPlugin>> cb);

	/**
	 * A project was build. The callback will get a snapshot of the build
	 * information
	 *
	 * @param cb the callback called when the project was build
	 */
	OnWorkspace build(Consumer<? super BuildInfo> cb);

	/**
	 * The properties of a project have changed. This is also fired when the
	 * workspace properties changed.
	 *
	 * @param cb callback
	 */
	OnWorkspace changedProject(Consumer<? super Project> cb);

	/**
	 * A run file was changed. // TODO think deeper if this s needed
	 *
	 * @param cb the callback called with the changed Run file
	 */
	OnWorkspace changedRun(Consumer<? super Run> cb);
}
