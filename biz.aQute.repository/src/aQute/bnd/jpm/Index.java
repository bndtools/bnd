package aQute.bnd.jpm;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import aQute.bnd.version.Version;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;
import aQute.lib.json.Decoder;
import aQute.lib.json.JSONCodec;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.library.Library;
import aQute.service.library.Library.RevisionRef;
import aQute.service.library.Revisions;
import aQute.service.reporter.Reporter;
import aQute.struct.struct;

public class Index {
	Reporter reporter = new ReporterAdapter(System.out);

	static public class Repo extends struct {
		public List<Library.RevisionRef>	revisionRefs	= list();
		public boolean						learning		= true;
		public boolean						recurse			= false;

		byte[]								synced;

		// Manually print this so that we
		// have 1 line per resource, making it easier on git
		// to compare.
		void write(Formatter format) throws Exception {
			codec.enc().indent(" ").to(format.out()).put(this);
			format.flush();
		}
	}

	private static final SortedSet<Version>						EMPTY_VERSIONS	= Collections
			.unmodifiableSortedSet(new TreeSet<Version>());

	private File												indexFile;

	private Map<String,TreeMap<Version,Library.RevisionRef>>	cache;

	private Repo												repo;

	public Index(File file) {
		this.indexFile = file;
	}

	static final JSONCodec	codec	= new JSONCodec();

	private boolean			dirty;

	private void init() throws Exception {
		if (repo == null) {
			cache = new TreeMap<String,TreeMap<Version,Library.RevisionRef>>();

			if (indexFile.isFile() && indexFile.length() > 100) {
				Decoder dec = codec.dec();
				try {
					repo = dec.from(indexFile).get(new TypeReference<Repo>() {});
					for (Library.RevisionRef r : repo.revisionRefs) {
						TreeMap<Version,Library.RevisionRef> map = cache.get(r.bsn);
						if (map == null) {
							map = new TreeMap<Version,Library.RevisionRef>(Collections.reverseOrder());
							cache.put(r.bsn, map);
						}
						Version v = toVersion(r.baseline, r.qualifier);
						map.put(v, r);
					}
				} finally {
					dec.close();
				}
			} else {
				repo = new Repo();
			}
		}
	}

	public Set<String> getBsns() throws Exception {
		init();
		return cache.keySet();
	}

	public SortedSet<Version> getVersions(String bsn) throws Exception {
		init();

		TreeMap<Version,Library.RevisionRef> map = cache.get(bsn);
		if (map == null)
			return EMPTY_VERSIONS;

		return map.navigableKeySet();
	}

	public boolean addRevision(Library.RevisionRef ref) throws Exception {
		init();
		Version v = toVersion(ref.baseline, ref.qualifier);
		TreeMap<Version,Library.RevisionRef> map = cache.get(ref.bsn);
		if (map == null) {
			map = new TreeMap<Version,Library.RevisionRef>();
			cache.put(ref.bsn, map);
		}

		map.put(v, ref);
		dirty = true;
		repo.revisionRefs.add(ref);
		return true;
	}

	@SuppressWarnings("deprecation")
	public Library.RevisionRef getRevisionRef(String bsn, Version version) throws Exception {
		init();
		Map<Version,Library.RevisionRef> map = cache.get(bsn);
		if (map == null)
			return null;

		// Fixup the change from ref.url to ref.urls ...
		boolean save = false;

		RevisionRef ref = map.get(version);
		if (ref == null) {
			return null;
		}

		if (ref.urls.isEmpty() && ref.url != null) {
			ref.urls.add(ref.url);
			ref.url = null;
			save = true;
		}
		// Another fixup, have file urls in the refs :-(

		if (Boolean.getBoolean("jpm4j.in.test") == false) {
			for (Iterator<URI> i = ref.urls.iterator(); i.hasNext();) {
				URI uri = i.next();
				if (uri.getScheme().equalsIgnoreCase("file")) {
					i.remove();
					save = true;
				}
			}
		}
		if (save)
			save();

		return ref;
	}

	public boolean delete(String bsn, Version v) throws Exception {
		init();
		Map<Version,Library.RevisionRef> map = cache.get(bsn);
		if (map != null) {
			try {
				Library.RevisionRef removed = map.remove(v);
				if (removed != null) {
					for (Iterator<RevisionRef> i = repo.revisionRefs.iterator(); i.hasNext();) {
						RevisionRef other = i.next();
						if (Arrays.equals(other.revision, removed.revision)) {
							i.remove();
						}
					}
					repo.revisionRefs.remove(removed);
					dirty = true;
					return true;
				}
			} finally {
				if (map.isEmpty())
					cache.remove(bsn);
			}
		}

		return false;
	}

	public void save(Writer out) throws Exception {
		init();
		repo.revisionRefs = getRevisionRefs();
		Formatter f = new Formatter(out);
		repo.write(f);
	}

	public void save(OutputStream out) throws Exception {
		save(new OutputStreamWriter(out));
	}

	public void save(File out) throws Exception {
		File tmp = IO.createTempFile(out.getParentFile(), out.getName(), ".tmp");
		FileWriter fout = new FileWriter(tmp);
		try {
			save(fout);
		} finally {
			fout.close();
		}
		IO.rename(tmp, out);
	}

	public void save() throws Exception {
		save(false);
	}

	void save(boolean force) throws Exception {
		if (dirty || force)
			save(indexFile);
		dirty = false;
	}

	static Version toVersion(String baseline, String qualifier) {
		if (qualifier == null || qualifier.isEmpty())
			return new Version(baseline);
		else
			return new Version(baseline + "." + qualifier);
	}

	public static Version toVersion(Library.RevisionRef ref) {
		return toVersion(ref.baseline, ref.qualifier);
	}

	public Library.RevisionRef getRevisionRef(byte[] sha) throws Exception {
		init();
		for (SortedMap<Version, ? extends RevisionRef> list : cache.values()) {
			for (RevisionRef r : list.values()) {
				if (Arrays.equals(sha, r.revision))
					return r;
			}
		}
		return null;
	}

	public List<RevisionRef> getRevisionRefs() throws Exception {
		init();
		List<RevisionRef> refs = new ArrayList<Library.RevisionRef>();
		for (SortedMap<Version, ? extends RevisionRef> list : cache.values()) {
			refs.addAll(list.values());
		}
		return refs;
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public boolean isLearning() throws Exception {
		init();
		return repo.learning;
	}

	public void setLearning(boolean learning) {
		dirty |= repo.learning != learning;
		repo.learning = learning;

	}

	public boolean isRecurse() {
		return repo.recurse;
	}

	public void setRecurse(boolean recurse) throws Exception {
		init();
		dirty |= repo.recurse != recurse;
		repo.recurse = recurse;
	}

	public boolean isDirty() {
		return dirty;
	}

	public Revisions getRevisions() throws Exception {
		init();
		Revisions revisions = new Revisions();
		for (RevisionRef ref : repo.revisionRefs)
			revisions.content.add(ref.revision);
		revisions._id = Revisions.checksum(revisions);
		return revisions;
	}

	public boolean isSynced() throws Exception {
		init();
		return repo.synced != null && Arrays.equals(repo.synced, getRevisions()._id);
	}

	public byte[] getSynced() {
		return repo.synced;
	}

	public void setSynced(byte[] revisions) throws Exception {
		repo.synced = revisions;
		save();
	}
}
