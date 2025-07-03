package org.bndtools.templating.jgit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.libg.tuple.Pair;
import bndtools.central.Central;

public class Cache {

	private final ConcurrentMap<URI, Pair<String, byte[]>> cache = new ConcurrentHashMap<>();

	public byte[] download(URI uri) throws Exception {
		byte[] data;
		try {
			HttpClient client = Central.getWorkspace()
				.getPlugin(HttpClient.class);

			Pair<String, byte[]> cachedTag = cache.get(uri);
			if (cachedTag == null) {
				// Not previously cached
				TaggedData td = client.connectTagged(uri.toURL());
				if (td == null || td.isNotFound())
					throw new FileNotFoundException("Not found");

				if (td.getInputStream() == null) {
					throwNoResponseError(td);
				}

				data = IO.read(td.getInputStream());
				if (td.getTag() != null)
					cache.put(uri, new Pair<>(td.getTag(), data));
			} else {
				// Previously cached with an ETag
				TaggedData td = client.connectTagged(uri.toURL(), cachedTag.getFirst());
				if (td == null || td.isNotFound())
					throw new FileNotFoundException("Not found");
				if (td.getResponseCode() == 304) {
					// unchanged
					data = cachedTag.getSecond();
				} else {
					// changed

					if (td.getInputStream() == null) {
						throwNoResponseError(td);
					}

					data = IO.read(td.getInputStream());
					if (td.getTag() == null) {
						// server now not giving an etag -> remove from cache
						cache.remove(uri);
					} else {
						// replace cache entry with new tag
						cache.put(uri, new Pair<>(td.getTag(), data));
					}
				}
			}
			return data;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private void throwNoResponseError(TaggedData td) throws IOException {
		throw new IOException("Error (HTTP " + td.getResponseCode() + ") - no response: " + td
			+ " (Check https://bnd.bndtools.org/instructions/connection_settings.html in case of connection or authentication errors.)");
	}

}
