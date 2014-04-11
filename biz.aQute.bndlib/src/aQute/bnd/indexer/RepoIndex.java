package aQute.bnd.indexer;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.resource.*;
import org.osgi.resource.Resource;

import aQute.bnd.indexer.analyzers.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.resource.*;

/**
 * The repository indexer. See OSGi Enterprise Specification 5.0.0, chapter 132.
 */
public class RepoIndex {
	final static String VERSION = "2.1";

	final List<ResourceAnalyzer> analyzers = new ArrayList<ResourceAnalyzer>();


	/**
	 * Constructor
	 * 
	 * @param log
	 *            the log service to use
	 */
	public RepoIndex() {
		analyzers.add( new BundleAnalyzer() );
		analyzers.add( new OSGiFrameworkAnalyzer() );
		analyzers.add( new SCRAnalyzer() );
		analyzers.add( new BlueprintAnalyzer() );
	}





	/*
	 * Index a file and return a resource for it.
	 */

	static public class IndexResult {
		public Resource	resource;
		public long	signature;
		
	}
	public IndexResult indexFile(File file, URI url) throws Exception {
		Jar jar = new Jar(file);

		IndexResult result = new IndexResult();
		result.resource = null;
		result.signature = getSignature();

		List<Capability> capabilities = new ArrayList<Capability>();
		List<Requirement> requirements = new ArrayList<Requirement>();
		ResourceBuilder rb = new ResourceBuilder();
		
		for (ResourceAnalyzer analyzer : analyzers) {
				analyzer.analyzeResource(jar, rb);
		}
		
		result.resource = rb.build();
		
		return result;
	}

	public long getSignature() {
		return VERSION.hashCode();
	}


//	private void doContent(Jar resource, MimeType mimeType, List<? super Capability> capabilities) throws Exception {
//		CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_CONTENT);
//
//		File source = resource.getSource();
//		if ( source == null)
//			throw new IllegalArgumentException("No file associated with resource");
//	
//		String sha = SHA256.digest(source).asHex();
//		builder.addAttribute(Namespaces.NS_CONTENT, sha);
//
//		String location = calculateLocation(resource);
//		builder.addAttribute(Namespaces.ATTR_CONTENT_URL, location);
//
//		long size = resource.getSize();
//		if (size > 0L)
//			builder.addAttribute(Namespaces.ATTR_CONTENT_SIZE, size);
//
//		builder.addAttribute(Namespaces.ATTR_CONTENT_MIME, mimeType.toString());
//
//		capabilities.add(builder.buildCapability());
//	}
}
