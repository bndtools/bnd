package org.osgi.service.indexer.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;

public class Utils {
	public static final String readStream(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream);
		StringBuilder result = new StringBuilder();
		
		char[] buf = new char[1024];
		int charsRead = reader.read(buf, 0, buf.length);
		while (charsRead > -1) {
			result.append(buf, 0, charsRead);
			charsRead = reader.read(buf, 0, buf.length);
		}
		
		return result.toString();
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
		List<Capability> result = new ArrayList<Capability>();
		
		for (Capability cap : caps) {
			if (namespace.equals(cap.getNamespace()))
				result.add(cap);
		}
		
		return result;
	}
	
	public static List<Requirement> findReqs(String namespace, Collection<Requirement> reqs) {
		List<Requirement> result = new ArrayList<Requirement>();
		
		for (Requirement req : reqs) {
			if (namespace.equals(req.getNamespace()))
				result.add(req);
		}
		
		return result;
	}
}
