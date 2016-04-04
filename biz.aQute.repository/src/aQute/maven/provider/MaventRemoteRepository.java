package aQute.maven.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.maven.provider.MetadataParser.SnapshotVersion;
import aQute.service.reporter.Reporter;

public class MaventRemoteRepository implements Closeable {
	final HttpClient						client;
	final String							base;
	final Map<Revision,RevisionMetadata>	revisions	= new ConcurrentHashMap<>();
	final Map<Program,ProgramMetadata>		programs	= new ConcurrentHashMap<>();
	final String							id;
	final File								root;
	final Reporter							reporter;

	public MaventRemoteRepository(File root, HttpClient client, String base, Reporter reporter) throws Exception {
		this.root = root;
		this.client = client;
		this.base = base;
		this.reporter = reporter;
		this.id = toName(base);
	}

	public static String toName(String uri) throws Exception {
		String s = SHA1.digest(uri.getBytes(StandardCharsets.UTF_8)).asHex();
		return s.substring(0, 8);
	}

	public TaggedData fetch(String path, File file) throws Exception {
		URL url = new URL(base + path);
		int n = 0;
		while (true)
			try {
				reporter.trace("Fetching %s", path);

				TaggedData tag = client.build().useCache(file).asTag().go(url);
				reporter.trace("Fetched %s", tag);
				if (tag.getState() == State.UPDATED) {

					// https://issues.sonatype.org/browse/NEXUS-4900

					if (!path.endsWith("/maven-metadata.xml")) {
						URL shaUrl = new URL(base + path + ".sha1");
						URL md5Url = new URL(base + path + ".md5");
						Promise<String> sha = client.build().asString().timeout(15000).async(shaUrl);
						Promise<String> md5 = client.build().asString().timeout(15000).async(md5Url);
						if (sha.getFailure() == null) {
							String fileSha = SHA1.digest(file).asHex();
							checkDigest(fileSha, sha.getValue(), file);
						} else if (md5.getFailure() == null) {
							String fileMD5 = MD5.digest(file).asHex();
							checkDigest(fileMD5, md5.getValue(), file);
						}
					}
				}
				return tag;
			} catch (Exception e) {
				n++;
				if (n > 3)
					throw e;
				Thread.sleep(1000 * n);
			}
	}

	private void checkDigest(String fileSha, String remoteSha, File file) {
		if (remoteSha == null)
			return;

		try {
			int start = 0;
			while (start < remoteSha.length() && Character.isWhitespace(remoteSha.charAt(start)))
				start++;

			for (int i = 0; i < fileSha.length(); i++) {
				if (start + i < remoteSha.length()) {
					char us = fileSha.charAt(i);
					char them = remoteSha.charAt(start + i);
					if (us == them || Character.toLowerCase(us) == Character.toLowerCase(them))
						continue;
				}
				throw new IllegalArgumentException("Invalid checksum content " + remoteSha + " for " + file);
			}

		} catch (Exception e) {
			file.delete();
			throw e;
		}
	}

	public void store(File file, String path) throws Exception {

		int n = 0;
		URL url = new URL(base + path);
		SHA1 sha1 = SHA1.digest(file);
		MD5 md5 = MD5.digest(file);

		Promise<String> psha = client.build()
				.put()
				.upload(sha1.asHex())
				.asString()
				.async(new URL(base + path + ".sha1"));
		Promise<String> pmd5 = client.build().put().upload(md5.asHex()).asString().async(new URL(base + path + ".md5"));

		TaggedData go = client.build().put().upload(file).updateTag().asTag().go(url);

		if (go.getResponseCode() != HttpURLConnection.HTTP_CREATED && go.getResponseCode() != HttpURLConnection.HTTP_OK)
			throw new IOException("Could not store " + path + " from " + file + " with " + go);

		if (psha.getFailure() != null)
			throw (Exception) psha.getFailure();

		if (pmd5.getFailure() != null)
			throw (Exception) psha.getFailure();

	}

	public boolean delete(String path) throws Exception {
		URL url = new URL(base + path);
		TaggedData go = client.build().put().delete().get(TaggedData.class).go(url);
		if (go == null)
			return false;

		if (go.getResponseCode() == HttpURLConnection.HTTP_OK
				|| go.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
			client.build().delete().async(new URL(base + path + ".sha1"));
			client.build().delete().async(new URL(base + path + ".md5"));
			return true;
		}

		throw new HttpRequestException(go);
	}

	public void close() {

	}

	public URI toURI(String remotePath) throws Exception {
		return new URI(base + remotePath);
	}

	public void getRevisions(Program program, List<Revision> revisions) throws Exception {
		ProgramMetadata meta = getMetadata(program);

		for (MavenVersion v : meta.versions) {
			revisions.add(program.version(v));
		}
	}

	RevisionMetadata getMetadata(Revision revision) throws Exception {
		File metafile = IO.getFile(root, revision.metadata(id));
		RevisionMetadata metadata = revisions.get(revision);

		URI url = new URI(base + revision.metadata());
		TaggedData tag = client.build().useCache(metafile).asTag().headers("User-Agent", "bnd").go(url);
		if (tag.getState() == State.NOT_FOUND || tag.getState() == State.OTHER) {
			if (metadata == null) {
				metadata = new RevisionMetadata();
				revisions.put(revision, metadata);
				return metadata;
			}
			throw new IOException("HTTP failed:" + tag.getResponseCode());
		}

		if (metadata == null || tag.getState() == State.UPDATED) {
			metadata = MetadataParser.parseRevisionMetadata(metafile);
			revisions.put(revision, metadata);
		}

		return metadata;
	}

	ProgramMetadata getMetadata(Program program) throws Exception {
		File metafile = IO.getFile(root, program.metadata(id));
		ProgramMetadata metadata = programs.get(program);

		TaggedData tag = client.build().useCache(metafile).asTag().go(new URI(base + program.metadata()));
		if (tag == null || tag.isOk() || metadata == null) {
			metadata = MetadataParser.parseProgramMetadata(metafile);
			programs.put(program, metadata);
		} else if (!tag.isNotModified())
			throw new IOException("HTTP failed:" + tag.getResponseCode());

		return metadata;
	}

	public List<Archive> getSnapshotArchives(Revision revision) throws Exception {
		RevisionMetadata metadata = getMetadata(revision);
		List<Archive> archives = new ArrayList<>();
		for (SnapshotVersion snapshotVersion : metadata.snapshotVersions) {
			Archive archive = revision.archive(snapshotVersion.value, snapshotVersion.extension,
					snapshotVersion.classifier);
			archives.add(archive);
		}

		return archives;
	}

	public MavenVersion getVersion(Revision revision) throws Exception {
		RevisionMetadata metadata = getMetadata(revision);
		if (metadata.snapshotVersions.isEmpty())
			return null;

		return revision.version.toSnapshot(metadata.snapshot.timestamp, metadata.snapshot.buildNumber);
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "RemoteRepo [base=" + base + ", id=" + id + "]";
	}
}
