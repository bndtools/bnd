package org.bndtools.core.editors.quickfix;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Descriptors;
import aQute.lib.exceptions.Exceptions;
import bndtools.Plugin;
import bndtools.central.Central;

public class BuildpathQuickFixProcessor implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		switch (problemId) {
			case IProblem.IsClassPathCorrect :
				// System.out.println("IsClassPathCorrect");
				return true;
			case IProblem.ImportNotFound :
				// System.out.println("ImportNotFound");
				return true;
			case IProblem.UndefinedType :
				// System.out.println("UndefinedType");
				return true;
			default :
				return false;
		}
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
		throws CoreException {
		try {
			List<IJavaCompletionProposal> proposals = new ArrayList<>();

			ICompilationUnit compUnit = context.getCompilationUnit();
			IJavaProject java = compUnit.getJavaProject();
			if (java == null)
				return null;

			Project project = Central.getProject(java.getProject());
			if (project == null)
				return null;

			boolean test = isInDir(project.getTestSrc(), compUnit.getResource());
			Workspace workspace = project.getWorkspace();

			for (IProblemLocation location : locations) {

				if (!hasCorrections(context.getCompilationUnit(), location.getProblemId()))
					continue;

				String partialClassName = getPartialClassName(location.getCoveringNode(context.getASTRoot()));
				if (partialClassName == null && location.getProblemArguments().length > 0)
					partialClassName = location.getProblemArguments()[0];

				if (partialClassName == null)
					continue;

				boolean doImport = Descriptors.determine(partialClassName)
					.map(sa -> sa[0] == null)
					.orElse(false);

				Map<String, List<BundleId>> result = workspace.search(partialClassName)
					.orElseThrow(s -> new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, s)));

				Set<BundleId> buildpath = getBundleIds(project.getBuildpath());
				Set<BundleId> testpath = test ? getBundleIds(project.getTestpath()) : Collections.emptySet();

				result.entrySet()
					.forEach(e -> {
						for (BundleId id : e.getValue()) {

							if (test && !testpath.contains(id) && !buildpath.contains(id))
								proposals.add(
									propose(e.getKey(), id, context, location, project, "-testpath", doImport));

							if (!buildpath.contains(id))
								proposals.add(propose(e.getKey(), id, context, location, project, "-buildpath",
									doImport));

						}
					});
			}
			return proposals.toArray(new IJavaCompletionProposal[0]);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private boolean isInDir(File dir, IResource resource) {
		if (resource == null || dir == null)
			return false;

		IPath location = resource.getLocation();
		if (location != null) {
			File file = location.toFile();
			return isInDir(dir, file);
		}
		return false;
	}

	private boolean isInDir(File dir, File file) {
		Path d = dir.toPath();
		Path f = file.toPath();
		return f.startsWith(d);
	}

	private Set<BundleId> getBundleIds(Collection<Container> collection) throws Exception {
		return collection.stream()
			.filter(c -> c.getError() == null)
			.map(Container::getBundleId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	private IJavaCompletionProposal propose(String proposalString, BundleId bundle, IInvocationContext context,
		IProblemLocation location, Project project, String type, boolean doImport) {

		return new AddBundleCompletionProposal(proposalString, bundle, 15, context, project, type, doImport);
	}

	/**
	 * Find out the fully qualified name of either a package or a type or an
	 * import declaration
	 *
	 * @param node the AST node that is causing the problem
	 * @return a partial fully qualified class name or null if this cannot be
	 *         established
	 */
	private String getPartialClassName(ASTNode node) {

		if (node instanceof ImportDeclaration) {
			node = ((ImportDeclaration) node).getName();
		}

		Name name = null;
		while (node instanceof Name) {
			name = (Name) node;
			node = node.getParent();
		}

		if (name == null)
			return null;
		return name.toString();
	}
}
