package bndtools.model.repo;

import java.net.URI;

import aQute.bnd.service.repository.SearchableRepository;

public class ContinueSearchElement {
	private final String				filter;
	private final SearchableRepository	repository;

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

	public URI browse() throws Exception {
		return repository.browse(filter);
	}

}
