package org.bndtools.templating.jgit;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.lib.json.JSONCodec;

public class GitHub {

    private static final String URL_PREFIX = "https://api.github.com/repos/";

    private final Cache cache;
    private final ExecutorService executor;

    public GitHub(Cache cache, ExecutorService executor) {
        this.cache = cache;
        this.executor = executor;
    }

    // TODO: use the async download service when available.
    public Promise<GithubRepoDetailsDTO> loadRepoDetails(final String repository) throws Exception {
        final Deferred<GithubRepoDetailsDTO> deferred = new Deferred<>();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] detailsDtoData = cache.download(URI.create(URL_PREFIX + repository));
                    GithubRepoDetailsDTO detailsDTO = new JSONCodec().dec().from(detailsDtoData).get(GithubRepoDetailsDTO.class);
                    deferred.resolve(detailsDTO);
                } catch (Exception e) {
                    deferred.fail(e);
                }
            }
        });
        return deferred.getPromise();
    }

}
