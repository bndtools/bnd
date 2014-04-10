package biz.aQute.resolve.repository;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.osgi.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;

public class TestRepository implements InfoRepository {
	final Map<String,SearchableRepository.ResourceDescriptor>	files	= new HashMap<String,SearchableRepository.ResourceDescriptor>();
	final MultiMap<String,Version>								index	= new MultiMap<String,Version>();
	final File													dir;

	public TestRepository(File dir) throws Exception {
		this.dir = dir;
		traverse(dir);
	}

	private void traverse(File dir) throws Exception {
		if (dir.isFile() && dir.getName().endsWith(".jar")) {
			Jar jar = new Jar(dir);
			Manifest m = jar.getManifest();
			String bsn = jar.getBsn();
			Version version = Version.parseVersion(jar.getVersion());
			if (m != null && bsn != null) {
				ResourceDescriptor rd = new ResourceDescriptor();
				rd.bsn = bsn;
				rd.version = version;
				rd.id = SHA1.digest(dir).digest();
				rd.description = m.getMainAttributes().getValue("Bundle-Description");
				rd.sha256 = SHA256.digest(dir).digest();
				rd.url = dir.toURI();
				files.put(Hex.toHexString(rd.id), rd);
				index.add(rd.bsn, rd.version);
			}
			jar.close();
		} else if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				traverse(f);
			}
		}
	}

	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException();
	}

	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		ResourceDescriptor descriptor = getDescriptor(bsn, version);
		if (descriptor == null)
			return null;

		URI url = descriptor.url;
		File f = IO.getFile(url.getPath());

		for (DownloadListener dl : listeners) {
			dl.success(f);
		}

		return f;
	}

	public boolean canWrite() {
		return false;
	}

	public List<String> list(String pattern) throws Exception {
		return new ArrayList<String>(index.keySet());
	}

	public SortedSet<Version> versions(String bsn) throws Exception {
		List<Version> list = index.get(bsn);
		if (list == null)
			list = Collections.emptyList();

		return new TreeSet<Version>(list);
	}

	public String getName() {
		return "test.repo";
	}

	public String getLocation() {
		return dir.getAbsolutePath();
	}

	public ResourceDescriptor getDescriptor(String bsn, Version version) throws Exception {
		for (ResourceDescriptor rd : files.values()) {
			if (rd.bsn.equals(bsn) && rd.version.equals(version)) {
				return rd;
			}
		}
		return null;
	}

}
