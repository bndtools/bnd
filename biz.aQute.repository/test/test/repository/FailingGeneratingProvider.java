package test.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Set;

import org.osgi.service.log.LogService;

import aQute.bnd.deployer.repository.api.CheckResult;
import aQute.bnd.deployer.repository.api.Decision;
import aQute.bnd.deployer.repository.api.IRepositoryContentProvider;
import aQute.bnd.deployer.repository.api.IRepositoryIndexProcessor;
import aQute.bnd.service.Registry;

public class FailingGeneratingProvider implements IRepositoryContentProvider {

	@Override
	public String getName() {
		return "Fail";
	}

	@Override
	public void parseIndex(InputStream stream, URI baseUrl, IRepositoryIndexProcessor listener, LogService log)
		throws Exception {}

	@Override
	public CheckResult checkStream(String name, InputStream stream) throws IOException {
		return new CheckResult(Decision.accept, "I accept anything but create nothing!", null);
	}

	@Override
	public boolean supportsGeneration() {
		return true;
	}

	@Override
	public void generateIndex(Set<File> files, OutputStream output, String repoName, URI rootUrl, boolean pretty,
		Registry registry, LogService log) throws Exception {
		throw new UnsupportedOperationException("This always breaks");
	}

	@Override
	public String getDefaultIndexName(boolean pretty) {
		return "neverhappens.xml";
	}

}
