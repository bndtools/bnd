package aQute.bnd.maven;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;

public class Maven {
	final File				userHome		= new File(System.getProperty("user.home"));
	final File				m2				= new File(userHome, ".m2");
	final File				repository		= new File(m2, "repository");
	final Map<String, Pom>	poms			= new ConcurrentHashMap<String, Pom>();
	final List<URL>			repositories	= new ArrayList<URL>();
	final String[]			ALGORITHMS		= { "md5", "sha1" };

	public Pom getPom(String groupId, String artifactId, String version, URL... extra)
			throws Exception {
		String pomPath = pom(groupId, artifactId, version);
		File pomFile = IO.getFile(repository, pomPath);

		Pom pom;
		synchronized (poms) {
			pom = poms.get(pomPath);
			if (pom != null)
				return pom;

			pom = new Pom(this);
			poms.put(pomPath, pom);

			if (pomFile.isFile()) {
				verify(pomFile);
				pom.setFile(pomFile);

				return pom;
			}
		}
		try {
			download(pomPath, pomFile);
			pom.setFile(pomFile);
			return pom;
		} catch (Exception e) {
			pom.exception = e;
			throw e;
		}
	}

	private void download(String path, File file, URL... extra) throws Exception {
		LinkedHashSet<URL> set = new LinkedHashSet<URL>();
		for (URL url : extra) {
			if (url != null)
				set.add(url);
		}
		set.addAll(repositories);

		if (set.isEmpty())
			set.add(new URL("http://repo1.maven.org/maven2/"));

		file.getParentFile().mkdirs();

		for (URL repo : set) {
			try {
				URL url = new URL(repo, path);
				System.out.println("Downloading " + url);
				IO.copy(url.openStream(), file);
				for (String alg : ALGORITHMS)
					try {
						IO.copy(new URL(url.toExternalForm() + "." + alg).openStream(), file);
					} catch (Exception e) {
						System.out.println("Failed to copy checksum " + alg + " " + e);
						// ignore
					}
			} catch (Exception e) {
				System.out.println("Download failed due to " + e);
				// Ignore, try next
			}
		}
		file.delete();
		throw new FileNotFoundException(path + " in " + set);
	}

	private void verify(File file) throws Exception {
		verify(file, MD5.getDigester());
		verify(file, SHA1.getDigester());
	}

	private void verify(File file, Digester<?> digester) throws Exception {
		File newFile = new File(file.getAbsolutePath() + "."
				+ digester.getAlgorithm().toLowerCase());
		if (!newFile.isFile()) {
			return;
		}

		IO.copy(file, digester);
		digester.flush();
		byte[] digest = digester.getMessageDigest().digest();
		String s = IO.collect(newFile).toUpperCase();
		String hex = Hex.toHexString(digest).toUpperCase();
		if (s.startsWith(hex)) {
			System.out.println("Verified ok " + file);
			return;
		}

		throw new InvalidObjectException(String.format("Failed to verify %s checksum for %s",
				digester.getAlgorithm(), file));
	}

	private String path(String groupId, String artifactId, String version) {
		return groupId.replace('.', '/') + '/' + artifactId + '/' + version;
	}

	private String pom(String groupId, String artifactId, String version) {
		return path(groupId, artifactId, version) + "/" + artifactId + "-" + version + ".pom";
	}

	private String artifact(String groupId, String artifactId, String version) {
		return path(groupId, artifactId, version) + "/" + artifactId + "-" + version + ".jar";
	}

	public File getArtifact(String groupId, String artifactId, String version, URL... urls)
			throws Exception {
		String path = artifact(groupId, artifactId, version);
		File file = IO.getFile(repository, path);
		if (file.isFile())
			return file;

		download(path, file, urls);
		return file;
	}
}
