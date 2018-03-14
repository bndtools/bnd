package org.bndtools.templating.jgit;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.lib.json.JSONCodec;

public class GitHub {

    private static final String URL_PREFIX = "https://api.github.com/repos/";

    private final Cache cache;
    private final PromiseFactory promiseFactory;

    public GitHub(Cache cache, Executor executor) {
        this.cache = cache;
        this.promiseFactory = new PromiseFactory(Objects.requireNonNull(executor));
    }

    public GitHub(Cache cache, PromiseFactory promiseFactory) {
        this.cache = cache;
        this.promiseFactory = promiseFactory;
    }

    // TODO: use the async download service when available.
    public Promise<GithubRepoDetailsDTO> loadRepoDetails(String repository) {
        return promiseFactory.submit(() -> {
            byte[] detailsDtoData = cache.download(URI.create(URL_PREFIX + repository));
            return new JSONCodec().dec()
                .from(detailsDtoData)
                .get(GithubRepoDetailsDTO.class);
        });
    }
}
