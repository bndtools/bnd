package org.bndtools.templating.jgit;

import java.net.URI;

import aQute.lib.json.JSONCodec;

public class GitHub {

    private static final String URL_PREFIX = "https://api.github.com/repos/";

    private final Cache cache;

    public GitHub(Cache cache) {
        this.cache = cache;
    }

    public GithubRepoDetailsDTO loadRepoDetails(String repository) throws Exception {
        byte[] detailsDtoData = cache.download(URI.create(URL_PREFIX + repository));
        GithubRepoDetailsDTO detailsDTO = new JSONCodec().dec().from(detailsDtoData).get(GithubRepoDetailsDTO.class);
        return detailsDTO;
    }

}
