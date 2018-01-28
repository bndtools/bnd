package org.osgi.service.indexer.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.junit.Ignore;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;

@Ignore
public class Utils {
	/** the platform specific EOL */
	static private String eol = String.format("%n");

	public static final String readStream(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
		try {
			StringBuilder result = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (result.length() > 0) {
					result.append(eol);
				}
				result.append(line);
			}
			return result.toString();
		} finally {
			reader.close();
		}
	}

	public static final String decompress(InputStream compressedStream) throws IOException {
		GZIPInputStream decompressedStream = new GZIPInputStream(compressedStream);
		return readStream(decompressedStream);
	}

	public static String decompress(String string) throws IOException {
		return decompress(new ByteArrayInputStream(string.getBytes()));
	}

	public static String decompress(byte[] byteArray) throws IOException {
		return decompress(new ByteArrayInputStream(byteArray));
	}

	public static List<Capability> findCaps(String namespace, Collection<Capability> caps) {
		List<Capability> result = new ArrayList<>();

		for (Capability cap : caps) {
			if (namespace.equals(cap.getNamespace()))
				result.add(cap);
		}

		return result;
	}

	public static List<Requirement> findReqs(String namespace, Collection<Requirement> reqs) {
		List<Requirement> result = new ArrayList<>();

		for (Requirement req : reqs) {
			if (namespace.equals(req.getNamespace()))
				result.add(req);
		}

		return result;
	}
}
