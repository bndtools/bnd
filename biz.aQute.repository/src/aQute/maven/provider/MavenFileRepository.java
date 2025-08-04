package aQute.maven.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.service.reporter.Reporter;

public class MavenFileRepository extends MavenBackingRepository {

	private final File remote;
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
     * Creates a ZIP archive containing all files from the remote repository directory.
     *
     * @param outputFile the destination file for the ZIP archive
     * @return the created ZIP file
     * @throws IOException if an error occurs during ZIP creation
     */
    public File createZipArchive(File outputFile) throws IOException {
        if (!remote.exists() || !remote.isDirectory()) {
            throw new IllegalStateException("Remote directory does not exist or is not a directory: " + remote);
        }

        IO.mkdirs(outputFile.getParentFile());

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            Path remotePath = remote.toPath();

            Files.walk(remotePath)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        Path relativePath = remotePath.relativize(path);
                        ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace("\\", "/"));

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
                        reporter.error("Error adding file %s to ZIP archive: %s", path, e.getMessage());
                    }
                });
        }

        return outputFile;
    }

    /**
     * Creates a ZIP archive containing all files from the remote repository directory.
     * The ZIP file will be created in the system temp directory with a generated name.
     *
     * @return the created ZIP file
     * @throws IOException if an error occurs during ZIP creation
     */
    public File createZipArchive() throws IOException {
		File tempFile = File.createTempFile("sonatype-bundle-", ".zip");
		tempFile.deleteOnExit();
        return createZipArchive(tempFile);
    }

	protected HttpClient getClient() {
		return client;
	}

	protected void setClient(HttpClient client) {
		this.client = client;
	}

}
