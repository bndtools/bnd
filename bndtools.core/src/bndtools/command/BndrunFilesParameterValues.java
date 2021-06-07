package bndtools.command;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import aQute.bnd.memoize.Memoize;
import bndtools.central.Central;

public class BndrunFilesParameterValues implements IParameterValues {

	static final String										BNDRUN_FILE		= "bnd.command.bndrunFile";

	private static IResourceChangeListener					listener		= new InvalidateMapListener();
	private volatile static Supplier<Map<String, String>>	bndrunFilesMap	= reset();

	static {
		ResourcesPlugin.getWorkspace()
			.addResourceChangeListener(listener);
	}

	static Supplier<Map<String, String>> reset() {
		return Memoize.supplier(() -> Arrays.stream(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects())
			.filter(IProject::isOpen)
			.map(BndrunFilesParameterValues::findBndrunFiles)
			.flatMap(Collection::stream)
			.map(Central::toBestPath)
			.map(Optional::get)
			.filter(Objects::nonNull)
			.distinct()
			.collect(toMap(IPath::toPortableString, IPath::toPortableString)));
	}

	@Override
	public Map getParameterValues() {
		return bndrunFilesMap.get();
	}

	private static List<IFile> findBndrunFiles(IProject project) {
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
				delta.accept(delta1 -> {
					String name = delta1.getFullPath()
						.lastSegment();
					if (name != null && name.endsWith(".bndrun") && (delta1.getKind() & kind) > 0) {
						bndrunChanged.set(true);
						return false;
					}
					return true;
				});
			} catch (CoreException e) {}

			if (bndrunChanged.get())
				bndrunFilesMap = BndrunFilesParameterValues.reset();
		}
	}
}
