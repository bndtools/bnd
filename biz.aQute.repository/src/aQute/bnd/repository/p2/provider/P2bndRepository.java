package aQute.bnd.repository.p2.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.service.reporter.Reporter;

public class P2bndRepository
		implements RepositoryPlugin, RegistryPlugin, Plugin, Closeable, Refreshable, InfoRepository, Actionable {

	@Override
	public Map<String,Runnable> actions(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String title(Object... target) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceDescriptor getDescriptor(String bsn, Version version) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean refresh() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public File getRoot() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void setReporter(Reporter processor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRegistry(Registry registry) {
		// TODO Auto-generated method stub

	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canWrite() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

}
