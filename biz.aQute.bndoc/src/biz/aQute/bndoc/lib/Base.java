package biz.aQute.bndoc.lib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.lib.env.*;

abstract class Base extends Env implements Constants, Closeable{
	protected Base(Base g) {
		super(g);
	}

	protected Base() {
	}

	protected void config() throws Exception {
	}

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

	private List<URI> expand(String key, Map<String,String> props) throws Exception {
		File f = getFile(key);
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
				getFiles(files,f);
				Sieve instr = new Sieve( props.get(FILTER));
				for ( File ff : instr.select(files, true)) {
					list.add(ff.toURI());
				}
			}
		} else {
			try {
				URL url = new URL(key);
				list.add(url.toURI());
			}
			catch (MalformedURLException e) {
				error("Unregocnized URI or file: %s", key);
			}
		}
		return list;
	}
	
	protected URI toURI( String path) throws URISyntaxException {
		File f = getFile(path);
		if ( f.isFile())
			return f.toURI();
		
		return new URI(path);
	}

	private void getFiles(List<File> files, File f) {
		if ( f.isFile())
			files.add(f);
		else {
			if ( f.getName().startsWith("."))
				return;
			
			for ( File sub : f.listFiles()) {
				getFiles(files,sub);
			}
		}
	}

	protected void prepare() throws Exception {
		super.prepare();
		
	}
}
