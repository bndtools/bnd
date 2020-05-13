package bndtools.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

import aQute.bnd.build.Workspace;
import bndtools.central.Central;

public class BndrunFilesParameterValues implements IParameterValues {

	static final String BNDRUN_FILE = "bnd.command.bndrunFile";

	private static Map<String, String> bndrunFilesMap;
	private static IResourceChangeListener	listener	= new InvalidateMapListener();

	@Override
	public Map getParameterValues() {
		Workspace ws = Central.getWorkspaceIfPresent();

		if (ws == null)
			return Collections.EMPTY_MAP;

		synchronized (ws) {
			if (bndrunFilesMap == null) {
				bndrunFilesMap = ws
					.getAllProjects()
					.stream()
					.map(Central::getProject)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.map(
						this::findBndrunFiles)
					.flatMap(Collection::stream)
					.map(IResource::getFullPath)
					.collect(Collectors.toMap(IPath::toPortableString,
						IPath::toPortableString));

				ResourcesPlugin.getWorkspace()
					.addResourceChangeListener(listener);
			}
		}

		return bndrunFilesMap;
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

	private static class InvalidateMapListener implements IResourceChangeListener {
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
				bndrunFilesMap = null;
		}
	}
}
