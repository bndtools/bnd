package org.bndtools.builder;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import bndtools.central.Central;

class DeltaWrapper {

	private static final IPath		EXT						= new Path("/cnf/ext");
	private final Project			model;
	private final IResourceDelta	delta;
	private final BuildLogger		log;

	static final String[]			defaultIgnoreProperties	= new String[] {
		Constants.DEFAULT_PROP_SRC_DIR,												//
		Constants.DEFAULT_PROP_TESTSRC_DIR,											//
		Constants.DEFAULT_PROP_TESTBIN_DIR,											//
		Constants.DEFAULT_PROP_TARGET_DIR
	};

	DeltaWrapper(Project model, IResourceDelta delta, BuildLogger log) {
		this.model = model;
		this.delta = delta;
		this.log = log;
	}

	boolean hasCnfChanged() throws Exception {
		if (delta == null) {
			log.basic("Full build because delta for cnf is null");
			return true;
		}

		if (havePropertiesChanged(model.getWorkspace())) {
			log.basic("cnf properties have changed");
			return true;
		}

		if (isNonMarkerChange(delta.findMember(EXT)))
			return true;

		return false;
	}

	boolean isNonMarkerChange(IResourceDelta delta) {
		if (delta == null)
			return false;
		int kindMask = IResourceDelta.CHANGED | IResourceDelta.ADDED | IResourceDelta.REMOVED;
		if ((kindMask & delta.getKind()) > 0) {
			int flags = delta.getFlags();
			if (flags > 0 && flags != IResourceDelta.MARKERS) {
				log.basic("Delta affects resource %s with kind=%d and flags=%d", delta.getFullPath(), delta.getKind(),
					delta.getFlags());
				return true;
			}
		}

		IResourceDelta[] children = delta.getAffectedChildren();
		if (children != null)
			for (IResourceDelta child : children) {
				if (isNonMarkerChange(child))
					return true;
			}

		return false;
	}

	/*
	 * Any change other then src, test, test_bin, or generated is fair game.
	 */
	boolean hasProjectChanged() throws Exception {

		if (delta == null) {
			log.basic("Full build because delta is null");
			return true;
		}

		if (havePropertiesChanged(model)) {
			log.basic("project properties have changed");
			return true;
		}

		final AtomicBoolean result = new AtomicBoolean(false);
		delta.accept(new IResourceDeltaVisitor() {
			Set<String> ignore = null;

			@Override
			public boolean visit(IResourceDelta visitDelta) throws CoreException {

				if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
					return false;

				IResource resource = visitDelta.getResource();
				if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
					return true;

				String path = resource.getProjectRelativePath()
					.toString();

				if (resource.getType() == IResource.FOLDER) {
					if (ignore == null) {
						ignore = Stream.concat(Arrays.stream(defaultIgnoreProperties)
							.map(model::getProperty), //
							Strings.splitAsStream(model.mergeProperties(Constants.BUILDERIGNORE)))
							.collect(toSet());
					}

					if (check(path, ignore.stream())) {
						return false;
					}
				}

				if (IResourceDelta.MARKERS == delta.getFlags())
					return false;

				log.basic("%s changed", resource);
				result.set(true);
				return false;
			}

		});

		return result.get();
	}

	boolean hasBuildfile() throws Exception {
		File f = new File(model.getTarget(), Constants.BUILDFILES);
		return has(f);
	}

	boolean havePropertiesChanged(Processor processor) throws Exception {

		if (has(processor.getPropertiesFile()))
			return true;

		for (File incl : processor.getIncluded()) {
			if (has(incl))
				return true;
		}

		return false;
	}

	private boolean has(File f) throws Exception {

		if (f == null)
			return false;

		if (delta == null)
			return false;

		IPath path = Central.toPath(f);
		if (path == null)
			return false;

		IPath relativePath = path.makeRelativeTo(delta.getFullPath());
		if (relativePath == null)
			return false;

		IResourceDelta delta = this.delta.findMember(relativePath);
		if (delta == null)
			return false;

		//
		// If ONLY the markers are changed we should ignore this delta
		// or get an infinite loop
		//

		if ((delta.getFlags() == IResourceDelta.MARKERS))
			return false;

		if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED
			|| delta.getKind() == IResourceDelta.REMOVED)
			return true;

		return false;
	}

	static boolean check(String changed, String prefix) {
		if (changed.equals(prefix))
			return true;

		if (changed.length() <= prefix.length())
			return false;

		char c = changed.charAt(prefix.length());
		if (c == '/' && changed.startsWith(prefix))
			return true;

		return false;
	}

	static boolean check(String changed, Stream<String> prefixes) {
		return prefixes.anyMatch(prefix -> check(changed, prefix));
	}

	public boolean isTestBin(IResource resource) {
		String path = resource.getProjectRelativePath()
			.toString();
		return check(path, model.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR));
	}

	/**
	 * Check if the target JARs have gone. We look for the buildfiles and the
	 * listed jars. If anything is odd, we rebuild.
	 */
	public boolean hasNoTarget(Project model) throws Exception {

		//
		// $/buildfiles must exists
		//

		File[] buildFiles = model.getBuildFiles(false);
		if (buildFiles == null)
			return true;

		File file = IO.getFile(model.getTarget(), Constants.BUILDFILES);
		if (!file.isFile())
			return true;

		//
		// buildfiles = line +
		//

		try {

			String bf = IO.collect(file);
			if (bf.isEmpty())
				return true;

			String[] split = bf.split("\r?\n");
			for (String line : split) {

				line = line.trim();
				if (line.startsWith("#"))
					continue;

				File f = IO.getFile(model.getTarget(), line);
				if (!f.isFile())
					return true;
			}

		} catch (Exception e) {
			return true;
		}

		return false;
	}

	public boolean hasChangedSubbundles() throws CoreException {
		if (delta == null)
			return true;

		final List<String> files = toFiles(false);
		Instructions instr = new Instructions(model.getProperty(Constants.SUB));
		Collection<String> selected = instr.select(files, false);
		return !selected.isEmpty();
	}

	private List<String> toFiles(final boolean dirsAlso) throws CoreException {
		if (delta == null)
			return null;

		final List<String> files = new ArrayList<>();

		delta.accept(d -> {
			IResource r = d.getResource();
			if (r != null) {
				if (r.getType() == IResource.PROJECT) {
					return true;
				}

				if (r.getType() == IResource.FILE) {
					files.add(d.getProjectRelativePath()
						.toString());
					return false;
				}
				if (r.getType() == IResource.FOLDER) {
					if (dirsAlso)
						files.add(d.getProjectRelativePath()
							.toString());

					return true;
				}
			}
			return false;
		});
		return files;
	}

	public boolean hasEclipseChanged() {
		if (delta == null)
			return true;
		IPath path = new Path(".classpath");
		return delta.findMember(path) != null;
	}
}
