package biz.aQute.resolve;

import aQute.bnd.osgi.resource.ResourceUtils;

/**
 * This marker must be implemented by repositories that model the workspace. If
 * things had gone well we would have had a single workspace repository.
 * However, due to history, a rather special workspace was integrated with
 * bndtools that did not make it into bnd itself. Worse, it depended on bndtools
 * code, making it impossible to integrate it in the rest of the code base.
 * <p>
 * When bnd started to resolve things, a special resolver was needed and this
 * was quickly hacked together. It was a static repository, it did not track the
 * interactive changes like the bndtools repository did. However, it was
 * confusing that sometimes bnd found a workspace repository (bndtools) and
 * sometimes not (the other drivers).
 * <p>
 * The purpose of this WorkspaceRepositoryMarker interface is to make it clear
 * to the resolver that a Workspace repository is present and should not be
 * added.
 * <p>
 * As a little bonus, any repository implementing this interface *must* add a
 * {@link ResourceUtils#WORKSPACE_NAMESPACE} capability to mark resources from
 * the workspace.
 * <p>
 * Ideally, bnd should have a single workspace repository that tracks the files
 * in the workspace but this is deemed too much work for now.
 */
public interface WorkspaceRepositoryMarker {
	String WORKSPACE_NAMESPACE = ResourceUtils.WORKSPACE_NAMESPACE;
}
