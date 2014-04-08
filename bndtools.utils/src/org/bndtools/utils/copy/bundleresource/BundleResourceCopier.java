package org.bndtools.utils.copy.bundleresource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

import aQute.lib.io.IO;

public class BundleResourceCopier {
	/** the bundle holding the resources */
	private Bundle bundle = null;

	/**
	 * Constructor
	 * 
	 * @param bundle
	 *            the bundle holding the resources
	 */
	public BundleResourceCopier(Bundle bundle) {
		super();
		this.bundle = bundle;
	}

	private void addOrRemoveDirectoryRecursive(File dstDir, String bundleDir, String relativePath, boolean add) throws IOException {
		String resourcePath = new File(bundleDir, relativePath).getPath();
		Enumeration<String> resourcePathEntries = bundle.getEntryPaths(resourcePath);
		if (resourcePathEntries != null) {
			while (resourcePathEntries.hasMoreElements()) {
				String resourcePathEntry = resourcePathEntries.nextElement();
				if (resourcePathEntry.startsWith(bundleDir)) {
					resourcePathEntry = resourcePathEntry.substring(bundleDir.length());
				}

				if (resourcePathEntry.endsWith("/")) {
					addOrRemoveDirectoryRecursive(dstDir, bundleDir, resourcePathEntry, add);
				} else {
					addOrRemoveFile(dstDir, bundleDir, resourcePathEntry, add);
				}
			}
		}
	}

	/**
	 * Add/remove a file (backed by a bundle resource) to/from a directory.
	 * 
	 * @param dstDir
	 *            the destination directory under which to add/remove a file
	 * @param bundleDir
	 *            the bundle directory under which the resource is located
	 * @param relativePath
	 *            the path of the resource (relative to bundleDir) in the
	 *            bundle. The resource will be added/removed to/from the same
	 *            path relative to dstDir. This parameter must only hold the
	 *            path of a file.
	 * @param add
	 *            true to add the file, false to remove it
	 * @throws IOException
	 *             when relativePath is null or empty, when the resource could
	 *             not be found in the bundle, when the directory holding the
	 *             file could not be created (when add is true), or when the
	 *             file could not be removed (when add is false)
	 */
	public void addOrRemoveFile(File dstDir, String bundleDir, String relativePath, boolean add) throws IOException {
		if (relativePath == null || relativePath.length() == 0) {
			throw new IOException("Resource relative path can't be empty");
		}

		File dstFile = new File(dstDir, relativePath);

		if (add) {
			String resourcePath = new File(bundleDir, relativePath).getPath();
			URL resourceUrl = bundle.getEntry(resourcePath);
			if (resourceUrl == null) {
				throw new IOException("Resource " + resourcePath + " not found in bundle " + bundle.getSymbolicName());
			}

			File dstFileDir = dstFile.getParentFile();
			if (dstFileDir != null) {
				boolean existsOrCreated = (dstFileDir.exists() && dstFileDir.isDirectory()) || dstFileDir.mkdirs();
				if (!existsOrCreated) {
					throw new IOException("Could not create directory " + dstFileDir.getAbsolutePath());
				}
			}

			IO.copy(resourceUrl, dstFile);
		} else {
			if (dstFile.exists() && !dstFile.delete()) {
				throw new IOException("Could not remove " + dstFile.getAbsolutePath());
			}
		}
	}

	/**
	 * Add/remove files (backed by bundle resources) to/from a directory.
	 * 
	 * @param dstDir
	 *            the destination directory under which to add/remove files
	 * @param bundleDir
	 *            the bundle directory under which the resources are located
	 * @param relativePaths
	 *            the paths of the resources (relative to bundleDir) in the
	 *            bundle. The resources will be added/removed to/from the same
	 *            paths relative to dstDir. This parameter must only hold paths
	 *            of files.
	 * @param add
	 *            true to add the files, false to remove them
	 * @throws IOException
	 *             when a relative path is null or empty, when a resource could
	 *             not be found in the bundle, when a directory holding a file
	 *             could not be created (when add is true), or when a file could
	 *             not be removed (when add is false)
	 */
	public void addOrRemoveFiles(File dstDir, String bundleDir, String[] relativePaths, boolean add) throws IOException {
		for (String templatePath : relativePaths) {
			addOrRemoveFile(dstDir, bundleDir, templatePath, add);
		}
	}

	/**
	 * Recursively add/remove a directory and its files (backed by bundle
	 * resources) to/from a directory.
	 * 
	 * @param dstDir
	 *            the destination directory under which to add/remove the
	 *            directory and its files
	 * @param bundleDir
	 *            the bundle directory under which the resources are located
	 * @param relativePath
	 *            the path of the resources (relative to bundleDir) in the
	 *            bundle. The resources will be recursively added/removed
	 *            to/from the same paths relative to dstDir. This parameter must
	 *            only hold a paths of a directory. When null then "/" will be
	 *            used.
	 * @param add
	 *            true to add the directory and its files, false to remove them
	 * @throws IOException
	 *             when a relative path is null or empty, when a resource could
	 *             not be found in the bundle, when a directory holding a file
	 *             could not be created (if add is true), or when a file could
	 *             not be removed (when add is false)
	 */
	public void addOrRemoveDirectory(File dstDir, String bundleDir, String relativePath, boolean add) throws IOException {
		String bundleDirFixed = bundleDir.replaceAll("^/+", "");
		if (!bundleDirFixed.endsWith("/")) {
			bundleDirFixed = bundleDirFixed + "/";
		}

		String relativePathFixed = relativePath;
		if (relativePathFixed == null) {
			relativePathFixed = "/";
		}
		if (!relativePathFixed.endsWith("/")) {
			relativePathFixed = relativePathFixed + "/";
		}

		addOrRemoveDirectoryRecursive(dstDir, bundleDirFixed, relativePathFixed, add);
	}

	/**
	 * Recursively add/remove directories and their files (backed by bundle
	 * resources) to/from a directory.
	 * 
	 * @param dstDir
	 *            the destination directory under which to add/remove
	 *            directories and files
	 * @param bundleDir
	 *            the bundle directory under which the resources are located
	 * @param relativePaths
	 *            the paths of the resources (relative to bundleDir) in the
	 *            bundle. The resources will be recursively added/removed
	 *            to/from the same paths relative to dstDir. This parameter must
	 *            only hold paths of directories.
	 * @param add
	 *            true to add the directories and their files, false to remove
	 *            them
	 * @throws IOException
	 *             when a relative path is null or empty, when a resource could
	 *             not be found in the bundle, when a directory holding a file
	 *             could not be created (when add is true), or when a file could
	 *             not be removed (when add is false)
	 */
	public void addOrRemoveDirectories(File dstDir, String bundleDir, String[] relativePaths, boolean add) throws IOException {
		for (String templatePath : relativePaths) {
			addOrRemoveDirectory(dstDir, bundleDir, templatePath, add);
		}
	}
}