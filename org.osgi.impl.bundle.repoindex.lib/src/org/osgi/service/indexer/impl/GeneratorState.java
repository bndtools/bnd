package org.osgi.service.indexer.impl;

import java.net.URI;
import java.net.URISyntaxException;

class GeneratorState {

	private final URI		rootUrl;
	private final String	urlTemplate;
	private URLResolver		resolver;

	public GeneratorState(URI rootUrl, String urlTemplate, URLResolver resolver) throws URISyntaxException {
		this.rootUrl = rootUrl;
		this.urlTemplate = urlTemplate;
		this.resolver = resolver;
	}

	URI getRootUrl() {
		return rootUrl;
	}

	String getUrlTemplate() {
		return urlTemplate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rootUrl == null) ? 0 : rootUrl.hashCode());
		result = prime * result + ((urlTemplate == null) ? 0 : urlTemplate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeneratorState other = (GeneratorState) obj;
		if (rootUrl == null) {
			if (other.rootUrl != null)
				return false;
		} else if (!rootUrl.equals(other.rootUrl))
			return false;
		if (urlTemplate == null) {
			if (other.urlTemplate != null)
				return false;
		} else if (!urlTemplate.equals(other.urlTemplate))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GeneratorState [rootUrl=" + rootUrl + ", urlTemplate=" + urlTemplate + "]";
	}

	public URLResolver getResolver() {
		return resolver;
	}
}
