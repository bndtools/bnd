package aQute.lib.deployer.repository.api;

import java.io.File;
import java.io.OutputStream;
import java.util.Set;

import org.osgi.service.log.LogService;
import org.xml.sax.ContentHandler;

import aQute.bnd.service.Registry;

public interface IRepositoryContentProvider {

	String getName();

	ContentHandler createContentHandler(String baseUrl, IRepositoryListener listener);

	void generateIndex(Set<File> files, OutputStream output, String repoName, String rootUrl, boolean pretty, Registry registry, LogService log) throws Exception;

	String getDefaultIndexName(boolean pretty);

}
