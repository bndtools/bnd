package aQute.bnd.build.api;

import java.io.Closeable;
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
public interface OnWorkspace extends Closeable {

	/**
	 * Callback when the workspace is initializing. This callback will be always
	 * directly and when the properties of the workspace change.
	 *
	 * @param cb the callback, will be called at least once
	 */
	OnWorkspace initial(Consumer<Workspace> cb);

	/**
	 * Callback when the workspace is initializing. This callback will be always
	 * directly and when the properties of the workspace change.
	 *
	 * @param cb the callback, will be called at least once
	 */
	OnWorkspace projects(Consumer<Collection<Project>> projects);

	OnWorkspace message(Consumer<Workspace> cb);

	OnWorkspace closing(Consumer<Workspace> cb);

	OnWorkspace repositoriesReady(Consumer<Collection<RepositoryPlugin>> cb);

	OnWorkspace build(Consumer<BuildInfo> cb);

	/**
	 * The properties of a project has changed
	 *
	 * @param cb callback
	 */
	OnWorkspace changedProject(Consumer<Project> cb);

	OnWorkspace changedRun(Consumer<? extends Run> cb);
}
