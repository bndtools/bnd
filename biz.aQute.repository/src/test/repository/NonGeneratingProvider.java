package test.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.service.log.*;

import aQute.bnd.deployer.repository.api.*;
import aQute.bnd.service.*;

class NonGeneratingProvider implements IRepositoryContentProvider {

	public String getName() {
		return "Nongenerating";
	}

	public void parseIndex(InputStream stream, URI baseUrl, IRepositoryIndexProcessor listener, LogService log)
			throws Exception {}

	public CheckResult checkStream(String name, InputStream stream) throws IOException {
		return new CheckResult(Decision.accept, "I accept anything but create nothing!", null);
	}

	public boolean supportsGeneration() {
		return false;
	}

	public void generateIndex(Set<File> files, OutputStream output, String repoName, URI rootUrl, boolean pretty,
			Registry registry, LogService log) throws Exception {
		throw new UnsupportedOperationException("I told you I don't support this!");
	}

	public String getDefaultIndexName(boolean pretty) {
		return "neverhappens.xml";
	}

}
