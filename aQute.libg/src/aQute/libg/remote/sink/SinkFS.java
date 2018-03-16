package aQute.libg.remote.sink;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import aQute.libg.remote.Delta;
import aQute.libg.remote.Source;

public class SinkFS {
	final Map<File, String>	shas	= new ConcurrentHashMap<>();
	final Map<String, File>	files	= new ConcurrentHashMap<>();
	private Source[]		sources;
	private File			shacache;

	public SinkFS(Source[] sources, File shacache) {
		this.shacache = shacache;
		setSources(sources);
	}

	public void setSources(Source[] sources) {
		this.sources = sources;
	}

	public boolean delta(File cwd, Collection<Delta> deltas) {
		for (Delta delta : deltas) {
			try {
				delta(cwd, delta);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

	private void delta(File cwd, Delta delta) throws Exception {
		File file = new File(delta.path);
		if (file.isAbsolute())
			throw new IllegalArgumentException("Absolute paths are not allowed " + delta.path);

		file = new File(cwd, delta.path);
		if (delta.delete)
			IO.delete(file);
		else {
			if (delta.sha != null) {
				String existing = shas.get(file);
				if (existing == null || !delta.sha.equals(existing)) {
					byte[] data = getData(delta.sha);
					if (data != null) {
						copy(data, file, delta.sha);
					} else
						shas.remove(file);
				}
			} else if (delta.content != null) {
				byte[] bytes = delta.content.getBytes(UTF_8);
				String sha = SHA1.digest(bytes)
					.asHex();
				copy(bytes, file, sha);
			}
		}
	}

	private void copy(byte[] data, File file, String sha) throws Exception {
		IO.mkdirs(file.getParentFile());
		IO.copy(data, file);
		shas.put(file, sha);
	}

	private byte[] getData(String sha) throws Exception {
		File shaf = new File(shacache, sha);
		if (shaf.isFile()) {
			return IO.read(shaf);
		}
		for (Source source : sources) {
			byte[] data = source.getData(sha);
			if (data != null) {
				File tmp = IO.createTempFile(shacache, "shacache", null);
				IO.copy(data, tmp);
				IO.rename(tmp, shaf);
				return data;
			}
		}
		return null;
	}

}
