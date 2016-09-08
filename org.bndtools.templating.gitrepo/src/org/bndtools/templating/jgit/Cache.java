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

public class Cache {

    private final ConcurrentMap<URI,Pair<String,byte[]>> cache = new ConcurrentHashMap<>();

    public byte[] download(URI uri) throws IOException {
        byte[] data;
        try (HttpClient client = new HttpClient()) {
            Pair<String,byte[]> cachedTag = cache.get(uri);
            if (cachedTag == null) {
                // Not previously cached
                TaggedData td = client.connectTagged(uri.toURL());
                if (td == null || td.isNotFound())
                    throw new FileNotFoundException("Not found");
                data = IO.read(td.getInputStream());
                if (td.getTag() != null)
                    cache.put(uri, new Pair<String,byte[]>(td.getTag(), data));
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
                    data = IO.read(td.getInputStream());
                    if (td.getTag() == null) {
                        // server now not giving an etag -> remove from cache
                        cache.remove(td.getTag());
                    } else {
                        // replace cache entry with new tag
                        cache.put(uri, new Pair<String,byte[]>(td.getTag(), data));
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

}
