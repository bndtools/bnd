package org.osgi.service.indexer.impl;

import static org.osgi.framework.FrameworkUtil.createFilter;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.types.TypedAttribute;
import org.osgi.service.indexer.impl.util.AddOnlyList;
import org.osgi.service.indexer.impl.util.Indent;
import org.osgi.service.indexer.impl.util.Pair;
import org.osgi.service.indexer.impl.util.Tag;
import org.osgi.service.log.LogService;

public class BIndex2 implements ResourceIndexer {
	
	static final String REPOSITORY_INCREMENT_OVERRIDE = "-repository.increment.override";
	
	private final BundleAnalyzer bundleAnalyzer = new BundleAnalyzer();
	private final OSGiFrameworkAnalyzer frameworkAnalyzer = new OSGiFrameworkAnalyzer();
	
	private final List<Pair<ResourceAnalyzer, Filter>> analyzers = new LinkedList<Pair<ResourceAnalyzer,Filter>>();
	
	private LogService log;
	
	public BIndex2() {
		try {
			Filter allFilter = createFilter("(name=*.jar)");
			
			addAnalyzer(bundleAnalyzer, allFilter);
			addAnalyzer(frameworkAnalyzer, allFilter);
			
		} catch (InvalidSyntaxException e) {
			// Can't happen...?
			throw new RuntimeException("Unexpected internal error compiling filter");
		}
	}
	
	public synchronized LogService getLog() {
		return log;
	}
	
	public synchronized void setLog(LogService log) {
		this.log = log;
	}
	
	public final void addAnalyzer(ResourceAnalyzer analyzer, Filter filter) {
		synchronized (analyzers) {
			analyzers.add(Pair.create(analyzer, filter));
		}
	}
	
	public final void removeAnalyzer(ResourceAnalyzer analyzer, Filter filter) {
		synchronized (analyzers) {
			analyzers.remove(Pair.create(analyzer, filter));
		}
	}

	public void index(Set<File> files, OutputStream out, Map<String, String> config) throws Exception {
		if (config == null)
			config = new HashMap<String, String>(0);
		
		Indent indent;
		PrintWriter pw;
		if (config.get(ResourceIndexer.PRETTY) != null) {
			indent = Indent.PRETTY;
			pw = new PrintWriter(out);
		} else {
			indent = Indent.NONE;
			pw = new PrintWriter(new GZIPOutputStream(out, Deflater.BEST_COMPRESSION));
		}
		
		pw.print(Schema.XML_PROCESSING_INSTRUCTION);
		Tag repoTag = new Tag(Schema.ELEM_REPOSITORY);
		
		String repoName = config.get(REPOSITORY_NAME);
		if (repoName == null)
			repoName = REPOSITORYNAME_DEFAULT;
		repoTag.addAttribute(Schema.ATTR_NAME, repoName);
		
		String increment = config.get(REPOSITORY_INCREMENT_OVERRIDE);
		if (increment == null)
			increment = Long.toString(System.currentTimeMillis());
		repoTag.addAttribute(Schema.ATTR_INCREMENT, increment);
		
		repoTag.addAttribute(Schema.ATTR_XML_NAMESPACE, Schema.NAMESPACE);
		
		repoTag.printOpen(indent, pw, false);
		for (File file : files) {
			Tag resourceTag = generateResource(file, config);
			resourceTag.print(indent.next(), pw);
		}
		repoTag.printClose(indent, pw);
		pw.flush(); pw.close();
	}

	public void indexFragment(Set<File> files, Writer out, Map<String, String> config) throws Exception {
		PrintWriter pw;
		if (out instanceof PrintWriter)
			pw = (PrintWriter) out;
		else
			pw = new PrintWriter(out);
		
		for (File file : files) {
			Tag resourceTag = generateResource(file, config);
			resourceTag.print(Indent.PRETTY, pw);
		}
	}
	
	private Tag generateResource(File file, Map<String, String> config) throws Exception {
		
		JarResource resource = new JarResource(file);
		List<Capability> caps = new AddOnlyList<Capability>(new LinkedList<Capability>());
		List<Requirement> reqs = new AddOnlyList<Requirement>(new LinkedList<Requirement>());
		
		// Read config settings and save in thread local state
		if (config != null) {
			URL rootURL;
			String rootURLStr = config.get(ResourceIndexer.ROOT_URL);
			if (rootURLStr != null) {
				File rootDir = new File(rootURLStr);
				if (rootDir.isDirectory())
					rootURL = rootDir.toURI().toURL();
				else
					rootURL = new URL(rootURLStr);
			}
			else
				rootURL = new File(System.getProperty("user.dir")).toURI().toURL();
			
			String urlTemplate = config.get(ResourceIndexer.URL_TEMPLATE);
			bundleAnalyzer.setStateLocal(new GeneratorState(rootURL, urlTemplate));
		} else {
			bundleAnalyzer.setStateLocal(null);
		}
		
		// Iterate over the analyzers
		try {
			synchronized (analyzers) {
				for (Pair<ResourceAnalyzer, Filter> entry : analyzers) {
					ResourceAnalyzer analyzer = entry.getFirst();
					Filter filter = entry.getSecond();
					
					if (filter == null || filter.match(resource.getProperties())) {
						try {
							analyzer.analyzeResource(resource, caps, reqs);
						} catch (Exception e) {
							log(LogService.LOG_ERROR, MessageFormat.format("Error calling analyzer \"{0}\" on resource {1}.", analyzer.getClass().getName(), resource.getLocation()), e);
						}
					}
				}
			}
		} finally {
			bundleAnalyzer.setStateLocal(null);
		}
		
		Tag resourceTag = new Tag(Schema.ELEM_RESOURCE);
		for (Capability cap : caps) {
			Tag capTag = new Tag(Schema.ELEM_CAPABILITY);
			capTag.addAttribute(Schema.ATTR_NAMESPACE, cap.getNamespace());
			
			appendAttributeAndDirectiveTags(capTag, cap.getAttributes(), cap.getDirectives());
			
			resourceTag.addContent(capTag);
		}
		
		for (Requirement req : reqs) {
			Tag reqTag = new Tag(Schema.ELEM_REQUIREMENT);
			reqTag.addAttribute(Schema.ATTR_NAMESPACE, req.getNamespace());
			
			appendAttributeAndDirectiveTags(reqTag, req.getAttributes(), req.getDirectives());
			
			resourceTag.addContent(reqTag);
		}
		
		return resourceTag;
	}

	private void log(int level, String message, Throwable t) {
		LogService log = getLog();
		if (log != null)
			log.log(level, message, t);
	}
	
	private static void appendAttributeAndDirectiveTags(Tag parentTag, Map<String, Object> attribs, Map<String, String> directives) {
		for (Entry<String, Object> attribEntry : attribs.entrySet()) {
			Tag attribTag = new Tag(Schema.ELEM_ATTRIBUTE);
			attribTag.addAttribute(Schema.ATTR_NAME, attribEntry.getKey());

			TypedAttribute typedAttrib = TypedAttribute.create(attribEntry.getKey(), attribEntry.getValue());
			parentTag.addContent(typedAttrib.toXML());
		}
		
		for (Entry<String, String> directiveEntry : directives.entrySet()) {
			Tag directiveTag = new Tag(Schema.ELEM_DIRECTIVE);
			directiveTag.addAttribute(Schema.ATTR_NAME, directiveEntry.getKey());
			directiveTag.addAttribute(Schema.ATTR_VALUE, directiveEntry.getValue());
			parentTag.addContent(directiveTag);
		}
	}
	
}
