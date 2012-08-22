package org.osgi.service.indexer.impl;

import java.net.URL;

class GeneratorState {

	private final URL rootUrl;
	private final String urlTemplate;

	public GeneratorState(URL rootUrl, String urlTemplate) {
		this.rootUrl = rootUrl;
		this.urlTemplate = urlTemplate;
	}

	public URL getRootUrl() {
		return rootUrl;
	}

	public String getUrlTemplate() {
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

}
