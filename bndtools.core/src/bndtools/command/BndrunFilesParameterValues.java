package bndtools.command;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import aQute.lib.memoize.Memoize;

public class BndrunFilesParameterValues implements IParameterValues {

	static final String BNDRUN_FILE = "bnd.command.bndrunFile";

	private IResourceChangeListener					listener	= new InvalidateMapListener();
	private volatile Supplier<Map<String, String>>	bndrunFilesMap;

	public BndrunFilesParameterValues() {
		bndrunFilesMap = reset();

		ResourcesPlugin.getWorkspace()
			.addResourceChangeListener(listener);
	}

	Supplier<Map<String, String>> reset() {
		return Memoize.supplier(() -> Arrays.stream(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects())
			.filter(IProject::isOpen)
			.map(this::findBndrunFiles)
			.flatMap(Collection::stream)
			.map(
				IResource::getFullPath)
			.collect(toMap(IPath::toPortableString, IPath::toPortableString)));
	}

	@Override
	public Map getParameterValues() {
		return bndrunFilesMap.get();
	}

	private List<IFile> findBndrunFiles(IProject project) {
		List<IFile> bndrunFiles = new ArrayList<>();

		try {
			project.accept(proxy -> {
				String name = proxy.getName();
				if (proxy.getType() == IResource.FILE && name.endsWith(".bndrun")) {
					bndrunFiles.add((IFile) proxy.requestResource());
					return false;
				}
				return true;
			}, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {}

		return bndrunFiles;
	}

	private class InvalidateMapListener implements IResourceChangeListener {
		private final int kind = IResourceDelta.ADDED | IResourceDelta.REMOVED;

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			final AtomicBoolean bndrunChanged = new AtomicBoolean(false);

			try {
				IResourceDelta delta = event.getDelta();
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						String name = delta.getFullPath()
							.lastSegment();
						if (name != null && name.endsWith(".bndrun") && (delta.getKind() & kind) > 0) {
							bndrunChanged.set(true);
							return false;
						}
						return true;
					}
				});
			} catch (CoreException e) {}

			if (bndrunChanged.get())
				bndrunFilesMap = BndrunFilesParameterValues.this.reset();
		}
	}
}
