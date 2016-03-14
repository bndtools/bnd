package aQute.maven.repo.provider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.Digest;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;

public class RemoteRepo {
	final HttpClient	client;
	final String		base;

	public RemoteRepo(HttpClient client, String base) {
		this.client = client;
		this.base = base;
	}

	public boolean fetch(String path, File file) throws Exception {
		int n = 0;
		while (true)
			try {
				URL url = new URL(base + path);
				Promise<String> sha = client.build().asString().async(new URL(base + path + ".sha1"));
				Promise<String> md5 = client.build().asString().async(new URL(base + path + ".md5"));
				TaggedData go = client.build().asTag().go(url);
				if (go == null)
					return false;

				if (go.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new HttpRequestException(go);

				file.getParentFile().mkdirs();
				IO.copy(go.getInputStream(), file);

				doChecksum("sha1", SHA1.digest(file), sha, file);
				doChecksum("md5", MD5.digest(file), md5, file);
				
				if (go.getModified() > 0)
					file.setLastModified(go.getModified());

				if (go.getTag() != null) {
					File etag = new File(file.getAbsolutePath() + ".etag");
					IO.store(go.getTag(), etag);
					if (go.getModified() > 0)
						etag.setLastModified(go.getModified());
				}

				return true;
			} catch (Exception e) {
				n++;
				if (n > 3)
					throw e;
				Thread.sleep(1000);
			}
	}

	void doChecksum(String type, Digest local, Promise<String> remote, File file)
			throws InvocationTargetException, InterruptedException, IOException {
		if (remote.getFailure() != null || remote.getValue() == null)
			return;

		String l = local.asHex();
		String r = Strings.trim(remote.getValue());
		if (l.equalsIgnoreCase(r)) {
			File checksumFile = new File(file.getAbsolutePath() + "." + type);
			IO.store(remote.getValue(), checksumFile);
		} else
			throw new IllegalStateException(
					"Invalid " + type + " remote=" + remote.getValue() + " local=" + local.asHex() + " for " + file);
	}

	public void store(File file, String path) throws Exception {
		int n = 0;
		URL url = new URL(base + path);
		SHA1 sha1 = SHA1.digest(file);
		MD5 md5 = MD5.digest(file);

		Promise<Object> psha = client.build().put().upload(sha1.asHex()).async(new URL(base + path + ".sha1"));
		Promise<Object> pmd5 = client.build().put().upload(md5.asHex()).async(new URL(base + path + ".md5"));

		TaggedData go = client.build().put().upload(file).get(TaggedData.class).go(url);

		psha.getFailure();
		pmd5.getFailure();

		if (go.getResponseCode() != HttpURLConnection.HTTP_CREATED && go.getResponseCode() != HttpURLConnection.HTTP_OK)
			throw new IOException("Could not store " + path + " from " + file + " with " + go);
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

}
