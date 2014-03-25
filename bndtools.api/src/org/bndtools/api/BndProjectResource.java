package org.bndtools.api;

import java.net.URL;
import java.util.Map;

public class BndProjectResource {
	private URL url = null;
	private Map<String, String> replaceRegularExpressions = null;

	public BndProjectResource() {
		super();
	}

	public BndProjectResource(URL url) {
		super();
		this.url = url;
	}

	public BndProjectResource(URL url,
			Map<String, String> replaceRegularExpressions) {
		super();
		this.url = url;
		this.replaceRegularExpressions = replaceRegularExpressions;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public Map<String, String> getReplaceRegularExpressions() {
		return replaceRegularExpressions;
	}

	public void setReplaceRegularExpressions(
			Map<String, String> replaceRegularExpressions) {
		this.replaceRegularExpressions = replaceRegularExpressions;
	}
}
