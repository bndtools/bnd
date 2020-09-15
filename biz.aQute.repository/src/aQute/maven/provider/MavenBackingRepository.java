package aQute.maven.provider;

import java.io.Closeable;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA1;
import aQute.service.reporter.Reporter;

/*
 * Provides a remote or local backing repository.
 */
abstract public class MavenBackingRepository implements Closeable {

	final String	id;
	final File		local;
	final Reporter	reporter;

	MavenBackingRepository(File root, String base, Reporter reporter) throws Exception {
		this.local = root;
		this.reporter = reporter;
		this.id = toName(base);
	}

	static public List<MavenBackingRepository> create(String urls, Reporter reporter, File localRepo, HttpClient client)
		throws Exception {
		if (urls == null)
			return Collections.emptyList();

		List<MavenBackingRepository> result = new ArrayList<>();
		List<String> parts = Strings.split(urls);
		for (String part : parts) {
			MavenBackingRepository mbr = getBackingRepository(part, reporter, localRepo, client);
			result.add(mbr);
		}
		return result;
	}

	static public MavenBackingRepository getBackingRepository(String url, Reporter reporter, File localRepo,
		HttpClient client) throws Exception {
		url = clean(url);
		URI uri = new URI(url);
		if (uri.getScheme() == null) {
			File file = IO.getFile(uri.getPath());
			uri = file.toURI();
		}
		if (uri.getScheme()
			.equalsIgnoreCase("file")) {
			File remote = new File(uri);
			return new MavenFileRepository(localRepo, remote, reporter);
		} else {
			return new MavenRemoteRepository(localRepo, client, url, reporter);
		}
	}

	static public String clean(String url) {
		if (url.endsWith("/"))
			return url;

		return url + "/";
	}


	static String toName(String uri) throws Exception {
		String s = SHA1.digest(uri.getBytes(StandardCharsets.UTF_8))
			.asHex();
		return s.substring(0, 8);
	}

	@Override
	public void close() {

	}

	String getId() {
		return id;
	}

	String getUser() throws Exception {
		return null;
	}

	abstract TaggedData fetch(String path, File file) throws Exception;

	abstract void store(File file, String path) throws Exception;

	abstract boolean delete(String path) throws Exception;

	abstract boolean isFile();

	abstract boolean isRemote();

	abstract URI toURI(String remotePath) throws Exception;

	@Override
	public abstract String toString();

}
