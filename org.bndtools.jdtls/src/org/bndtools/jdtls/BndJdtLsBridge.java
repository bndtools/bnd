package org.bndtools.jdtls;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;

import org.bndtools.jdtls.dto.BndPackageInfo;
import org.bndtools.jdtls.dto.BndProjectInfo;

public class BndJdtLsBridge {
	private final BndWorkspaceDetector detector = new BndWorkspaceDetector();

	public Optional<BndProjectInfo> getProjectInfo(File file) throws Exception {
		Project project = getProject(file);
		if (project == null) {
			return Optional.empty();
		}
		return Optional.of(toProjectInfo(project));
	}

	public List<BndProjectInfo> getWorkspaceProjects(File file) throws Exception {
		Workspace workspace = getWorkspace(file);
		if (workspace == null) {
			return List.of();
		}

		List<BndProjectInfo> result = new ArrayList<>();
		for (Project project : workspace.getAllProjects()) {
			result.add(toProjectInfo(project));
		}
		result.sort(Comparator.comparing(BndProjectInfo::getName));
		return result;
	}

	public List<BndPackageInfo> getPackages(File file) throws Exception {
		Project project = getProject(file);
		if (project == null) {
			return List.of();
		}

		List<BndPackageInfo> result = new ArrayList<>();
		collectPackages(result, project.getSourcePath(), false);
		collectPackages(result, List.of(project.getTestSrc()), true);
		result.sort(Comparator.comparing(BndPackageInfo::getPackageName));
		return result;
	}

	public void refreshWorkspace(File file) throws Exception {
		Workspace workspace = getWorkspace(file);
		if (workspace != null) {
			workspace.refresh();
		}
	}

	private Workspace getWorkspace(File file) throws Exception {
		Optional<File> root = detector.findWorkspaceRoot(file);
		if (root.isEmpty()) {
			return null;
		}
		return Workspace.getWorkspace(root.get());
	}

	private Project getProject(File file) throws Exception {
		Workspace workspace = getWorkspace(file);
		if (workspace == null) {
			return null;
		}

		File target = file != null && file.isFile() ? file.getParentFile() : file;
		while (target != null && !target.equals(workspace.getBase())) {
			if (detector.isBndProject(target)) {
				return workspace.getProject(target.getName());
			}
			target = target.getParentFile();
		}
		return null;
	}

	private BndProjectInfo toProjectInfo(Project project) throws Exception {
		return new BndProjectInfo(project.getName(), absolute(project.getBase()), absolute(project.getPropertiesFile()),
			absolute(project.getOutput()), absolute(project.getTestOutput()), absolute(project.getSourcePath()),
			List.of(absolute(project.getTestSrc())), containers(project.getBuildpath()), containers(project.getTestpath()));
	}

	private static List<String> containers(Collection<Container> containers) throws Exception {
		if (containers == null || containers.isEmpty()) {
			return List.of();
		}

		List<String> result = new ArrayList<>();
		for (Container container : Container.flatten(containers)) {
			File file = container.getFile();
			if (file != null) {
				result.add(absolute(file));
			}
		}
		return result;
	}

	private static List<String> absolute(Collection<File> files) {
		if (files == null || files.isEmpty()) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (File file : files) {
			result.add(absolute(file));
		}
		return result;
	}

	private static String absolute(File file) {
		return file == null ? null : IO.absolutePath(file);
	}

	private static void collectPackages(List<BndPackageInfo> result, Collection<File> sourceRoots, boolean testPackage) {
		Set<String> seen = new LinkedHashSet<>();
		for (File sourceRoot : sourceRoots) {
			if (sourceRoot != null && sourceRoot.isDirectory()) {
				collectPackages(result, seen, sourceRoot, sourceRoot, testPackage);
			}
		}
	}

	private static void collectPackages(List<BndPackageInfo> result, Set<String> seen, File root, File current,
		boolean testPackage) {
		File[] children = current.listFiles();
		if (children == null) {
			return;
		}

		boolean hasJava = false;
		for (File child : children) {
			if (child.isFile() && child.getName()
				.endsWith(".java")) {
				hasJava = true;
				break;
			}
		}

		if (hasJava) {
			String packageName = root.toPath()
				.relativize(current.toPath())
				.toString()
				.replace(File.separatorChar, '.');
			if (!packageName.isEmpty() && seen.add(packageName)) {
				result.add(new BndPackageInfo(packageName, absolute(root), testPackage));
			}
		}

		for (File child : children) {
			if (child.isDirectory()) {
				collectPackages(result, seen, root, child, testPackage);
			}
		}
	}
}
