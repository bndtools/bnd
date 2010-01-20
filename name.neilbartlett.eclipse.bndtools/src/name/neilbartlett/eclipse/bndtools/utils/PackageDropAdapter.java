package name.neilbartlett.eclipse.bndtools.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.ResourceTransfer;

public abstract class PackageDropAdapter<T> extends ViewerDropAdapter {
	
	private final TableViewer viewer;
	private final List<T> modelList;
	
	public PackageDropAdapter(TableViewer viewer, List<T> modelList) {
		super(viewer);
		this.viewer = viewer;
		this.modelList = modelList;
	}
	
	@Override
	public void dragEnter(DropTargetEvent event) {
		event.detail = DND.DROP_COPY;
		super.dragEnter(event);
	}
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return TextTransfer.getInstance().isSupportedType(transferType)
			|| ResourceTransfer.getInstance().isSupportedType(transferType);
	}
	@Override
	public void dropAccept(DropTargetEvent event) {
		if (event.data instanceof IResource[]) {
			// Fail if there is not at least one IPackageFragment in the selection
			IResource[] resources = (IResource[]) event.data;
			for (IResource resource : resources) {
				IJavaElement element = JavaCore.create(resource);
				if(element != null && element instanceof IPackageFragment)
					return;
			}
			event.detail = DND.DROP_NONE;
		}
	}
	
	protected abstract T createNew(String packageName);
	
	protected abstract void rowsAdded(Collection<T> rows);
	
	@Override
	public boolean performDrop(Object data) {
		int insertionIndex = -1;
		Object target = getCurrentTarget();
		if(target != null) {
			insertionIndex = modelList.indexOf(target);
			int loc = getCurrentLocation();
			if(loc == LOCATION_ON || loc == LOCATION_AFTER)
				insertionIndex++;
		}
		
		List<T> newEntries = new ArrayList<T>();
		if(data instanceof String) {
			String stringData = (String) data;
			StringTokenizer tok = new StringTokenizer(stringData, ",");
			while(tok.hasMoreTokens()) {
				String pkgName = tok.nextToken().trim();
				newEntries.add(createNew(pkgName));
			}
		} else if(data instanceof IResource[]) {
			for (IResource resource : (IResource[]) data) {
				IJavaElement javaElement = JavaCore.create(resource);
				if(javaElement instanceof IPackageFragment) {
					newEntries.add(createNew(javaElement.getElementName()));
				}
			}
		}

		if(insertionIndex == -1) {
			modelList.addAll(newEntries);
			viewer.add(newEntries.toArray());
		} else {
			modelList.addAll(insertionIndex, newEntries);
			viewer.refresh();
		}
		viewer.setSelection(new StructuredSelection(newEntries));
		
		rowsAdded(newEntries);
		return true;
	}
}
