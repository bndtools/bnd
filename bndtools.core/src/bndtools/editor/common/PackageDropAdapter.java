package bndtools.editor.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.ResourceTransfer;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import bndtools.model.resolution.RequirementWrapper;

public abstract class PackageDropAdapter<T> extends ViewerDropAdapter {

	private static final String		PACKAGE_FILTER_PATTERN	= "osgi.wiring.package=([^)]*)";

	private static final Pattern	pkgFilterPattern		= Pattern.compile(PACKAGE_FILTER_PATTERN);

	public PackageDropAdapter(Viewer viewer) {
		super(viewer);
	}

	protected abstract T createNewEntry(String packageName);

	protected abstract void addRows(int index, Collection<T> rows);

	protected abstract int indexOf(Object object);

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return ResourceTransfer.getInstance()
			.isSupportedType(transferType)
			|| LocalSelectionTransfer.getTransfer()
				.isSupportedType(transferType);
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		super.dragEnter(event);
		event.detail = DND.DROP_COPY;
	}

	@Override
	public boolean performDrop(Object data) {
		int insertionIndex = -1;
		Object target = getCurrentTarget();
		if (target != null) {
			insertionIndex = indexOf(target);
			int loc = getCurrentLocation();
			if (loc == LOCATION_ON || loc == LOCATION_AFTER)
				insertionIndex++;
		}

		List<T> newEntries = new ArrayList<>();
		if (data instanceof IResource[]) {
			for (IResource resource : (IResource[]) data) {
				IJavaElement javaElement = JavaCore.create(resource);
				if (javaElement instanceof IPackageFragment) {
					newEntries.add(createNewEntry(javaElement.getElementName()));
				}
			}
		} else if (data instanceof IStructuredSelection) {
			Iterator<?> iterator = ((IStructuredSelection) data).iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof IPackageFragment) {
					IPackageFragment pkg = (IPackageFragment) element;
					newEntries.add(createNewEntry(pkg.getElementName()));
				} else if (element instanceof Capability) {
					Capability cap = (Capability) element;
					String namespace = cap.getNamespace();
					if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
						String pkgName = (String) cap.getAttributes()
							.get(namespace);
						newEntries.add(createNewEntry(pkgName));
					}
				} else if (element instanceof Requirement) {
					String pkgName = getPackageNameFromRequirement((Requirement) element);
					if (pkgName != null)
						newEntries.add(createNewEntry(pkgName));
				} else if (element instanceof RequirementWrapper) {
					String pkgName = getPackageNameFromRequirement(((RequirementWrapper) element).requirement);
					if (pkgName != null)
						newEntries.add(createNewEntry(pkgName));
				}
			}
		}
		addRows(insertionIndex, newEntries);
		return true;
	}

	private String getPackageNameFromRequirement(Requirement req) {
		String ns = req.getNamespace();
		if (!PackageNamespace.PACKAGE_NAMESPACE.equals(ns))
			return null;

		String filterStr = req.getDirectives()
			.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		Matcher matcher = pkgFilterPattern.matcher(filterStr);
		if (!matcher.find())
			return null;

		return matcher.group(1);
	}
}
