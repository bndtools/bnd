package bndtools.launch.sourcelookup.containers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Run;
import aQute.bnd.exceptions.SupplierWithException;

public class BndrunDirectiveSourceContainer extends CompositeSourceContainer {
	private static final ILogger		logger	= Logger.getLogger(BndrunDirectiveSourceContainer.class);

	public static final String			TYPE_ID	= "org.bndtools.core.launch.sourceContainerTypes.bndrunDirective";

	final private String				directive;

	final private SupplierWithException<Collection<Container>>	directiveGetter;

	final boolean						containsBundles;

	public BndrunDirectiveSourceContainer(Run run, String directive) {
		if (directive == null) {
			throw new NullPointerException("directive should not be null");
		}
		this.directive = directive;
		switch (directive) {
			case "runbundles" :
				directiveGetter = run::getRunbundles;
				containsBundles = true;
				break;
			case "runpath" :
				directiveGetter = run::getRunpath;
				containsBundles = false;
				break;
			case "runfw" :
				directiveGetter = run::getRunFw;
				containsBundles = false;
				break;
			default :
				throw new IllegalArgumentException("Invalid bndrun directive: " + directive);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BndrunDirectiveSourceContainer
			&& ((BndrunDirectiveSourceContainer) obj).directive.equals(directive);
	}

	@Override
	public int hashCode() {
		return directive.hashCode();
	}

	@Override
	public String getName() {
		return directive;
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	final private static ISourceContainer[] EMPTY_SOURCE = new ISourceContainer[0];

	@Override
	protected ISourceContainer[] createSourceContainers() {
		Set<String> projectsAdded = new HashSet<>();
		try {
			return directiveGetter.get()
				.stream()
				.map(container -> {
					if (container.getType() == TYPE.PROJECT) {
						String targetProjName = container.getProject()
							.getName();
						if (projectsAdded.add(targetProjName)) {
							IProject targetProj = ResourcesPlugin.getWorkspace()
								.getRoot()
								.getProject(targetProjName);
							if (targetProj != null) {
								IJavaProject targetJavaProj = JavaCore.create(targetProj);
								return new JavaProjectSourceContainer(targetJavaProj);
							}
						}
					} else if (container.getType() == TYPE.REPO) {
						if (containsBundles) {
							return new BundleSourceContainer(container);
						} else {
							return new ExternalArchiveSourceContainer(container.getFile()
								.toString(), false);
						}
					}
					return null;
				})
				.filter(Objects::nonNull)
				.toArray(ISourceContainer[]::new);
		} catch (Exception e) {
			logger.logError("Error querying Bnd dependency source containers.", e);
		}

		return EMPTY_SOURCE;
	}
}
