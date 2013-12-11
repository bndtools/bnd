package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.lib.env.*;
import aQute.lib.io.*;

/**
 * Base class for bndoc specific environment handling.
 */
class Base extends Env implements Constants, Closeable {

	protected Base(Env main) {
		super(main);
	}

	protected Base() {}

	/**
	 * Convert a header to a map with URIs and properties. Directories are
	 * expanded.
	 * 
	 * @param header
	 * @return
	 * @throws Exception
	 */
	protected Map<URI,Props> toURis(String header) throws Exception {
		Map<URI,Props> map = new LinkedHashMap<>();
		Header p = getHeader(header);

		for (Entry<String,Props> entry : p.entrySet()) {
			List<URI> uris = expand(entry.getKey(), entry.getValue());
			if (uris == null || uris.isEmpty()) {
				error("Cannot find %s", uris);
			} else {
				for (URI uri : uris) {
					map.put(uri, entry.getValue());
				}
			}
		}
		return map;
	}

	/**
	 * Check if the target is a directory, if so expand recursively. Otherwise
	 * if it is a file, turn it in a URI, otherwise try to turn it into an absolute
	 * URI.
	 * 
	 * @param target
	 * @param props
	 * @return
	 * @throws Exception
	 */
	private List<URI> expand(String target, Map<String,String> props) throws Exception {
		File f = getFile(target);
		List<URI> list = new ArrayList<>();

		if (f.exists()) {
			if (f.isFile())
				list.add(f.toURI());
			else {

				int levels = 1;
				if (props.get(LEVELS) != null) {
					levels = Integer.parseInt(props.get(LEVELS));
				}
				List<File> files = new ArrayList<>();
				getFiles(files, f);
				Sieve instr = new Sieve(props.get(FILTER));
				for (File ff : instr.select(files, true)) {
					list.add(ff.toURI());
				}
			}
		} else {
			try {
				URL url = new URL(target);
				list.add(url.toURI());
			}
			catch (MalformedURLException e) {
				error("Unregocnized URI or file: %s", target);
			}
		}
		return list;
	}

	/**
	 * Turn a path into a URI
	 * 
	 * @param path
	 * @return
	 * @throws URISyntaxException
	 */
	protected URI toURI(String path) throws URISyntaxException {
		File f = getFile(path);
		if (f.isFile())
			return f.toURI();

		return new URI(path);
	}

	/**
	 * Add files recursively
	 * 
	 * @param files
	 * @param f
	 */
	private void getFiles(List<File> files, File f) {
		if (f.isFile())
			files.add(f);
		else {
			if (f.getName().startsWith("."))
				return;

			for (File sub : f.listFiles()) {
				getFiles(files, sub);
			}
		}
	}

	/**
	 * Return the content of a URI
	 * 
	 * @param uri
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected String getContent(URI uri) throws MalformedURLException, IOException {
		return IO.collect(uri.toURL().openStream());
	}

	protected File getFile(String propertyName, boolean dir, String defaultName) {
		File file;
		file = getFile(getProperty(propertyName, defaultName));

		file.getParentFile().mkdirs();
		if (dir) {
			file.mkdir();
			if (!file.isDirectory())
				error("Cannot create directory %s for %s", file, propertyName);
		}
		return file;
	}

	@Override
	public void close() throws IOException {
	}
}
