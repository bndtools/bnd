package aQute.maven.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.service.reporter.Reporter;

public class MavenFileRepository extends MavenBackingRepository {

	private final File	remote;
	private HttpClient	client	= null;

	public MavenFileRepository(File localRepo, File remote, Reporter reporter) throws Exception {
		super(localRepo, remote.toURI()
			.toString(), reporter);
		this.remote = remote;
	}

	@Override
	public TaggedData fetch(String path, File dest, boolean force) throws Exception {
		File source = getFile(path);
		if (source.isFile()) {
			IO.mkdirs(dest.getParentFile());
			IO.delete(dest);
			IO.copy(source, dest);
			return new TaggedData(toURI(path), 200, dest);
		} else {
			return new TaggedData(toURI(path), 404, dest);
		}
	}

	@Override
	public void store(File source, String path) throws Exception {
		if (!source.isFile())
			throw new IllegalArgumentException("File does not exist: " + source);

		File dest = getFile(path);

		IO.mkdirs(dest.getParentFile());
		IO.copy(source, dest);

		SHA1 sha1 = SHA1.digest(source);
		MD5 md5 = MD5.digest(source);
		IO.store(sha1.asHex() + "\n", new File(dest.getParentFile(), dest.getName() + ".sha1"));
		IO.store(md5.asHex() + "\n", new File(dest.getParentFile(), dest.getName() + ".md5"));
	}

	@Override
	public boolean delete(String path) throws Exception {
		File dest = getFile(path);
		IO.deleteWithException(dest);
		return true;
	}

	@Override
	public String toString() {
		return "MavenFileRepo[" + remote + "]";
	}

	@Override
	public URI toURI(String path) throws Exception {
		File result = getFile(path);
		return result.toURI();
	}

	private File getFile(String path) {
		return IO.getFile(remote, path);
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	/**
	 * Discovers all groupIds in the Maven repository by analyzing the directory
	 * structure.
	 *
	 * @return map of groupId to its directory path
	 * @throws IOException if an error occurs reading the directory
	 */
	private Map<String, File> discoverGroupIds() throws IOException {
		Map<String, File> groupIds = new LinkedHashMap<>();
		if (!remote.exists() || !remote.isDirectory()) {
			return groupIds;
		}

		// Walk the directory tree to find all groupIds
		Files.walk(remote.toPath())
			.filter(Files::isDirectory)
			.forEach(dir -> {
				File dirFile = dir.toFile();
				File[] children = dirFile.listFiles();
				if (children != null && children.length > 0) {
					// Look for artifact directories (contain version
					// subdirectories with artifacts)
					for (File child : children) {
						if (child.isDirectory()) {
							File[] versionDirs = child.listFiles();
							if (versionDirs != null) {
								for (File versionDir : versionDirs) {
									if (versionDir.isDirectory() && containsArtifacts(versionDir)) {
										// Found a groupId directory
										Path relativePath = remote.toPath()
											.relativize(dir);
										String groupId = relativePath.toString()
											.replace(File.separatorChar, '.');
										if (!groupId.isEmpty() && !groupIds.containsKey(groupId)) {
											groupIds.put(groupId, dirFile);
										}
										return;
									}
								}
							}
						}
					}
				}
			});

		return groupIds;
	}

	/**
	 * Checks if a directory contains Maven artifacts (JAR, POM, WAR, AAR
	 * files).
	 */
	private boolean containsArtifacts(File dir) {
		File[] files = dir.listFiles();
		if (files == null) {
			return false;
		}
		for (File file : files) {
			if (file.isFile()) {
				String name = file.getName();
				if (name.endsWith(".jar") || name.endsWith(".pom") || name.endsWith(".war") || name.endsWith(".aar")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Creates ZIP archives, one for each Maven namespace/groupId in the
	 * repository. Each archive contains all files for that specific groupId.
	 *
	 * @return list of created ZIP files with groupId information
	 * @throws IOException if an error occurs during ZIP creation
	 */
	public List<GroupIdArchive> createZipArchive() throws IOException {
		if (!remote.exists() || !remote.isDirectory()) {
			throw new IllegalStateException("Remote directory does not exist or is not a directory: " + remote);
		}

		List<GroupIdArchive> archives = new ArrayList<>();
		Map<String, File> groupIds = discoverGroupIds();

		if (groupIds.isEmpty()) {
			reporter.warning("No groupIds found in repository: %s", remote);
			return archives;
		}

		reporter.trace("Found %d groupId(s) in repository: %s", groupIds.size(), groupIds.keySet());

		// Create a separate ZIP archive for each groupId
		for (Map.Entry<String, File> entry : groupIds.entrySet()) {
			String groupId = entry.getKey();
			File groupDir = entry.getValue();

			String sanitizedGroupId = groupId.replace('.', '_');
			File tempFile = File.createTempFile("sonatype-bundle-" + sanitizedGroupId + "-", ".zip");
			tempFile.deleteOnExit();

			try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
				Path groupDirPath = groupDir.toPath();
				Path remotePath = remote.toPath();

				Files.walk(groupDirPath)
					.filter(path -> !Files.isDirectory(path))
					.forEach(path -> {
						try {
							// Use path relative to remote root (includes
							// groupId path)
							Path relativePath = remotePath.relativize(path);
							ZipEntry zipEntry = new ZipEntry(relativePath.toString()
								.replace("\\", "/"));

							zos.putNextEntry(zipEntry);

							try (FileInputStream fis = new FileInputStream(path.toFile())) {
								byte[] buffer = new byte[4096];
								int len;
								while ((len = fis.read(buffer)) > 0) {
									zos.write(buffer, 0, len);
								}
							}

							zos.closeEntry();
						} catch (IOException e) {
							reporter.error("Error adding file %s to ZIP archive for groupId %s: %s", path, groupId,
								e.getMessage());
						}
					});
			}

			archives.add(new GroupIdArchive(groupId, tempFile));
			reporter.trace("Created ZIP archive for groupId %s: %s", groupId, tempFile);
		}

		return archives;
	}

	/**
	 * Container class for a groupId and its corresponding archive file.
	 */
	public static class GroupIdArchive {
		public final String	groupId;
		public final File	archiveFile;

		public GroupIdArchive(String groupId, File archiveFile) {
			this.groupId = groupId;
			this.archiveFile = archiveFile;
		}

		public String getSanitizedGroupId() {
			return groupId.replace('.', '_');
		}
	}

	protected HttpClient getClient() {
		return client;
	}

	protected void setClient(HttpClient client) {
		this.client = client;
	}

}
