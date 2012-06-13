package test.repository;

import java.io.*;
import java.util.*;

import org.osgi.service.log.*;

import aQute.bnd.service.*;
import aQute.lib.deployer.repository.api.*;

class FailingGeneratingProvider implements IRepositoryContentProvider {

	public String getName() {
		return "Fail";
	}

	public void parseIndex(InputStream stream, String baseUrl, IRepositoryListener listener, LogService log)
			throws Exception {}

	public CheckResult checkStream(String name, InputStream stream) throws IOException {
		return new CheckResult(Decision.accept, "I accept anything but create nothing!", null);
	}

	public boolean supportsGeneration() {
		return true;
	}

	public void generateIndex(Set<File> files, OutputStream output, String repoName, String rootUrl, boolean pretty,
			Registry registry, LogService log) throws Exception {
		throw new UnsupportedOperationException("This always breaks");
	}

	public String getDefaultIndexName(boolean pretty) {
		return "neverhappens.xml";
	}

}
