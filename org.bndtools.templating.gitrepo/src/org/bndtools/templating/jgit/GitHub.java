package org.bndtools.templating.jgit;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.osgi.util.promise.Promise;

import aQute.lib.json.JSONCodec;
import aQute.lib.promise.PromiseExecutor;

public class GitHub {

    private static final String URL_PREFIX = "https://api.github.com/repos/";

    private final Cache cache;
    private final PromiseExecutor executor;

    public GitHub(Cache cache, ExecutorService executor) {
        this.cache = cache;
        this.executor = new PromiseExecutor(executor);
    }

    // TODO: use the async download service when available.
    public Promise<GithubRepoDetailsDTO> loadRepoDetails(final String repository) throws Exception {
        return executor.submit(new Callable<GithubRepoDetailsDTO>() {
            @Override
            public GithubRepoDetailsDTO call() throws Exception {
                byte[] detailsDtoData = cache.download(URI.create(URL_PREFIX + repository));
                return new JSONCodec().dec().from(detailsDtoData).get(GithubRepoDetailsDTO.class);
            }
        });
    }

}
