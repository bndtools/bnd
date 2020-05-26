package bndtools.javasearch;

import java.util.Optional;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import bndtools.central.Central;

public class BndGeneratedJarFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return !Optional.ofNullable(element)
			.filter(IMember.class::isInstance)
			.map(IMember.class::cast)
			.filter(IMember::isBinary)
			.filter(member -> Optional.ofNullable(member.getResource())
				.map(IResource::getProject)
				.filter(Central::isBndProject)
				.isPresent())
			.filter(member -> Optional.ofNullable(member.getPath())
				.filter(this::isDerived)
				.map(IPath::lastSegment)
				.filter(file -> file.endsWith(".jar"))
				.isPresent())
			.isPresent();
	}

	private boolean isDerived(IPath path) {
		IResource resource = ResourcesPlugin.getWorkspace()
			.getRoot()
			.findMember(path);
		return resource != null && resource.isDerived(IResource.CHECK_ANCESTORS);
	}
}
