package aQute.bnd.osgi;

import java.io.*;
import java.util.*;

public class CombinedResource extends WriteResource {
	final List<Resource>	resources		= new ArrayList<Resource>();
	long					lastModified	= 0;

	@Override
	public void write(final OutputStream out) throws IOException, Exception {
		OutputStream unclosable = new FilterOutputStream(out) {
			public void close() {
				// Ignore
			}
		};
		for (Resource r : resources) {
			r.write(unclosable);
			unclosable.flush();
		}
	}

	@Override
	public long lastModified() {
		return lastModified;
	}

	public void addResource(Resource r) {
		lastModified = Math.max(lastModified, r.lastModified());
		resources.add(r);
	}

}
