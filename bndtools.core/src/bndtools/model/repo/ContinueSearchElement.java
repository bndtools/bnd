package bndtools.model.repo;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;

import aQute.bnd.service.repository.SearchableRepository;

public class ContinueSearchElement {

    private static final String DEFAULT_URL_PREFIX = "http://repo.jpm4j.org/#!/search?q=";

    private final String filter;
    private final SearchableRepository repository;

    public ContinueSearchElement(String filter, SearchableRepository repository) {
        this.filter = filter;
        this.repository = repository;
    }

    public String getFilter() {
        return filter;
    }

    public SearchableRepository getRepository() {
        return repository;
    }

    public URI browse() {
        // The browse() method was added to the JPM Repository plugin in version 1.3
        // of the package. Use reflection to avoid problems with out of date plugins.
        try {
            Method meth = SearchableRepository.class.getDeclaredMethod("browse", new Class[] {
                String.class
            });
            Object uri = meth.invoke(repository, filter);
            return (URI) uri;
        } catch (Exception e0) {
            try {
                // Default to the main jpm4j.org URL.
                return URI.create(DEFAULT_URL_PREFIX + URLEncoder.encode(filter, "UTF-8"));
            } catch (UnsupportedEncodingException e1) {
                // stupid Java!
                throw new RuntimeException(e1);
            }
        }
    }

}
