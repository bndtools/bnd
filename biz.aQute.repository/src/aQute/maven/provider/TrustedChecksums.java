package aQute.maven.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import aQute.lib.date.Dates;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.libg.cryptography.SHA512;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;

/**
 * Helper for handling trusted checksum verification for Maven artifacts
 */
public final class TrustedChecksums {

	private final File						sidecarFile;
	private Map<Archive, ArtifactChecksum>	index	= Map.of();
	private volatile boolean							indexExists				= false;
	private final Map<TrustedChecksumCacheKey, Boolean>	trustedChecksumCache	= new ConcurrentHashMap<>();

	public TrustedChecksums(File trustedChecksumFile) {
		this.sidecarFile = trustedChecksumFile;
	}

	public File getFile() {
		return this.sidecarFile;
	}

	/**
	 * Fetches a checksum for an Archive. Returns <code>null</code> if no entry
	 * exists.
	 *
	 * @param archive
	 * @return the ArtifactChecksum or <code>null</code> if not found.
	 */
	public ArtifactChecksum get(Archive archive) {
		if (!indexExists)
			return null;
		return index.get(archive);
	}

	/**
	 * Validates a downloaded artifact against a configured trusted checksum.
	 *
	 * @param archive the repository artifact archive
	 * @param file the downloaded artifact file
	 * @return {@code true} if a trusted checksum exists for the artifact and
	 *         the file successfully matches it; {@code false} if no trusted
	 *         checksum is configured
	 * @throws TrustedChecksumException if checksum calculation fails or the
	 *             file does not match the configured trusted checksum. In the
	 *             mismatch case the file is deleted before the exception is
	 *             thrown.
	 */
	public boolean checkTrustedChecksum(Archive archive, File file) {
		if (!indexExists)
			return false;

		ArtifactChecksum expected = get(archive);
		if (expected == null) {
			return false;
		}

		TrustedChecksumCacheKey key = TrustedChecksumCacheKey.from(archive, file, expected);

		// cache expensive computeHash
		Boolean cached = trustedChecksumCache.get(key);
		if (cached != null) {
			return cached;
		}

		// Reuse existing checkDigest() with format: "algorithm=hash"
		try {
			String fileHash = computeHash(file, expected.hashType());
			DigestValidator.checkDigest(file, fileHash, expected.hash());
			trustedChecksumCache.put(key, Boolean.TRUE);
			return true;
		} catch (Exception e) {
			throw new TrustedChecksumException(e.getMessage(), e);
		}
	}

	public void open() throws IOException {
		if (sidecarFile == null || !sidecarFile.isFile()) {
			return;
		}
		indexExists = true;
		Collection<ArtifactChecksum> set = read(IO.collect(sidecarFile));
		index = set.stream()
			.collect(Collectors.toMap(acs -> acs.archive(), acs -> acs));

	}

	public static void createTrustedChecksumFile(IMavenRepo repo, File checksumFile, Collection<Archive> archives)
		throws Exception {

		if (checksumFile == null || archives == null) {
			return;
		}

		if (checksumFile.isDirectory()) {
			throw new IllegalArgumentException("Trusted Checksum file must not be a directory: " + checksumFile);
		}

		try (FileOutputStream fos = new FileOutputStream(
			checksumFile);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

			writer.write("# Generated (" + Dates.formatMillis(DateTimeFormatter
				.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ROOT)
				.withZone(Dates.UTC_ZONE_ID), new Date().getTime())
				+ "): Trusted Checksums for each GAV in the maven index");
			writer.newLine();

			List<Archive> sorted = archives.stream()
				.sorted(java.util.Comparator.comparing(Archive::toString))
				.toList();

			for (Archive arch : sorted) {
				File localFile = repo.toLocalFile(arch);
				String line = arch.toString() + "=sha1:" + SHA1.digest(localFile)
					.asHex();
				writer.write(line);
				writer.newLine();
			}
			writer.flush();
		}
	}

	/**
	 * Parses the content of a .checksums file and returns the ArtifactCheckSum
	 * entries.
	 *
	 * @param source content of the trusted checksum file as a String
	 * @return the parsed de-duplicated ArtifactChecksum entries
	 */
	public static List<ArtifactChecksum> read(String source) {
		Set<ArtifactChecksum> archives = Strings.splitLinesAsStream(source)
			.map(s -> ArtifactChecksum.parse(s))
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		return List.copyOf(archives);
	}

	record ArtifactChecksum(Archive archive, String hashType, String hash) {
		public static ArtifactChecksum parse(String line) {

			line = line.trim();

			if (line.startsWith("#") || line.isEmpty())
				return null;

			String[] parts = line.split("=", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid checksum line format: " + line);
			}

			Archive archive = Archive.valueOf(parts[0].trim());

			String rhs = parts[1].trim();
			int colon = rhs.indexOf(':');
			if (colon < 0) {
				throw new IllegalArgumentException("Missing hash type separator: " + line);
			}

			String hashType = rhs.substring(0, colon)
				.trim();
			String hash = rhs.substring(colon + 1)
				.trim();

			return new ArtifactChecksum(archive, hashType, hash);
		}
	}

	public static String computeHash(File file, String hashType) throws Exception {
		return switch (hashType.toLowerCase()) {
			case "sha-256", "sha256" -> SHA256.digest(file)
				.asHex();
			case "sha-512", "sha512" -> SHA512.digest(file)
				.asHex();
			case "md5" -> MD5.digest(file)
				.asHex();
			case "sha1", "sha-1" -> SHA1.digest(file)
				.asHex();
			default -> throw new IllegalArgumentException("Unsupported hash type: " + hashType);
		};
	}

	@Override
	public String toString() {
		return sidecarFile != null ? String.valueOf(sidecarFile) : "-";
	}


	public static class TrustedChecksumException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public TrustedChecksumException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	private record TrustedChecksumCacheKey(Archive archive, File file, long lastModified, long length,
		String hashType,
		String expectedHash) {
		public static TrustedChecksumCacheKey from(Archive archive, File file, ArtifactChecksum expected) {
			return new TrustedChecksumCacheKey(archive, file.getAbsoluteFile(), file.lastModified(), file.length(),
				expected.hashType(), expected.hash());
		}
	}
}
